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
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;

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
        return IOUtils.toString(message.getInputStream(), "UTF-8");
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
