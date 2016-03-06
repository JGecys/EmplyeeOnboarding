package com.hackergames.emplyeeonboarding;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.sree.textbytes.jtopia.Configuration;
import com.sree.textbytes.jtopia.TermDocument;
import com.sree.textbytes.jtopia.TermsExtractor;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.*;

@Service
public class GmailService {

    @Autowired
    OAuth2RestTemplate template;

    @Autowired
    OAuth2ClientContext context;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;


    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Gmail service() {
        GoogleCredential credential = new GoogleCredential().setAccessToken(context.getAccessToken().getValue());
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName("EmplyeeOnboarding").build();
    }

    public List<Label> getLabels() throws IOException {
        return service().users().labels().list("me").execute().getLabels();
    }

    public List<Message> getMessages() throws IOException { // TODO: 2016-03-05  make it async
        String user = "me";
        Gmail service = service();
        List<String> labels = Collections.singletonList("INBOX");
        ListMessagesResponse response = service.users().messages()
                .list(user).setLabelIds(labels)
                .execute();

        List<Message> messages = new ArrayList<>();
        while (response.getMessages() != null) {
            messages.addAll(response.getMessages());
            if (response.getNextPageToken() != null) {
                String pageToken = response.getNextPageToken();
                response = service.users().messages()
                        .list(user).setLabelIds(labels).setPageToken(pageToken)
                        .execute();
            } else {
                break;
            }
        }
        return messages;
    }

    public MimeMessage getMessage(String messageId) throws IOException, MessagingException {
        Message msg = service().users().messages().get("me", messageId).setFormat("raw").execute();
        byte[] emailBytes = Base64.decodeBase64(msg.getRaw());
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        return new MimeMessage(session, new ByteArrayInputStream(emailBytes));
    }


    public String getMessageCategory(String messageId) throws IOException, MessagingException {
        MimeMessage message = getMessage(messageId);
        InputStream in = new FileInputStream("/training/modelFile.train");
        DoccatModel model = new DoccatModel(in); // TODO: 2016-03-06 Needs training data

        String msg = IOUtils.toString(message.getInputStream(), "UTF-8");

        DocumentCategorizerME myCategorizer = new DocumentCategorizerME(model);
        double[] outcomes = myCategorizer.categorize(msg);
        return myCategorizer.getBestCategory(outcomes);
    }

    public String getMessageContent(String messageId) throws IOException, MessagingException {
        MimeMessage message = getMessage(messageId);
//        String msg = IOUtils.toString(message.getInputStream(), "UTF-8");
//        EmailUtils.extractClearText(m, null);
        String msg = getClearText(message);
//        if(message.getContentType().equals("text/html")){
//            return Jsoup.parse(msg).body().text();
//        }
//        if(message.getContentType().equals("text/plain")){
//            return msg;
//        }
//        Document document = Jsoup.parse(msg);
//        document.select("a").remove();
//        document.body().text();
        msg = msg.replaceAll("https?://\\S+\\s?", "");
        return msg;
    }

    private String getClearText(MimeMessage message) throws MessagingException, IOException {
        MimeMessage m = (MimeMessage) message;
        Object contentObject = m.getContent();
        StringBuilder result = new StringBuilder();
        if (contentObject instanceof Multipart) {
            BodyPart clearTextPart = null;
            BodyPart htmlTextPart = null;
            Multipart content = (Multipart) contentObject;
            int count = content.getCount();
            for (int i = 0; i < count; i++) {
                BodyPart part = content.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    clearTextPart = part;
                } else if (part.isMimeType("text/html")) {
                    htmlTextPart = part;
                }
            }

            if (clearTextPart != null) {
                result.append((String) clearTextPart.getContent());
            }
            if (htmlTextPart != null) {
                String html = (String) htmlTextPart.getContent();
                result.append(Jsoup.parse(html).body().text());
            }

        } else if (contentObject instanceof String) // a simple text message
        {
            if(m.getContentType().startsWith("text/html")){
                return Jsoup.parse((String) contentObject).body().text();
            }
            return (String) contentObject;
        }
        return result.toString();
    }

    public List<String> getKeywords(String messageId) throws IOException, MessagingException {
        String content = getMessageContent(messageId);
        if (content.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Configuration.setTaggerType("openNLP");
        Configuration.setSingleStrength((int) Math.max(2, Math.pow(content.length() , 0.5d)/ 12));
        Configuration.setNoLimitStrength(5);
        // if tagger type is "openNLP" then give the openNLP POS tagger path
        Configuration.setModelFileLocation("model/openNLP/en-pos-maxent.bin");
        // if tagger type is "default" then give the default POS lexicon file
        //Configuration.setModelFileLocation("model/default/english-lexicon.txt");
        TermsExtractor termExtractor = new TermsExtractor();
        TermDocument topiaDoc = new TermDocument();
        topiaDoc = termExtractor.extractTerms(content);
        //logger.info("Extracted terms : " + topiaDoc.getExtractedTerms());
        Map<String, ArrayList<Integer>> finalFilteredTerms = topiaDoc.getFinalFilteredTerms();
        List<String> keywords = new ArrayList<>();
        for (Map.Entry<String, ArrayList<Integer>> e : finalFilteredTerms.entrySet()) {
            keywords.add(e.getKey());
        }
        return keywords;
    }

    public void train(String messageId) throws IOException, MessagingException {

        MimeMessage message = getMessage(messageId);
        String modelFilePath = "/training/modelFile.train";
        // Instance of openNLP's default model class
        DoccatModel model = null;
        InputStream dataIn = null;
        try {
            dataIn = message.getInputStream();
            ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn, "UTF-8");
            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);
            // "en" is language code of English.
            model = DocumentCategorizerME.train("en", sampleStream);
        } catch (IOException e) {
            System.out.println("Failed to read or parse training data, training failed");
        } finally {
            if (dataIn != null) {
                try {
                    dataIn.close();
                } catch (IOException e) {
                    System.out.println(e.getLocalizedMessage());
                }
            }
        }
        OutputStream modelOut = null;
        try {
            modelOut = new BufferedOutputStream(new FileOutputStream(modelFilePath));
            model.serialize(modelOut);
        } catch (IOException e) {
            System.out.println("Failed to save model at location " + modelFilePath);
        } finally {
            if (modelOut != null) {
                try {
                    modelOut.close();
                } catch (IOException e) {
                    System.out.println("Failed to correctly save model. Written model might be invalid.");
                }
            }
        }
    }


}
