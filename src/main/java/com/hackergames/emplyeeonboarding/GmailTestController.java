package com.hackergames.emplyeeonboarding;

import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.mail.Address;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

@RestController
public class GmailTestController {

    @Autowired
    GmailService service;

    @RequestMapping("/labels")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public List<Label> getLabels() throws IOException {
        return service.getLabels();
    }

    @RequestMapping("/messages")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public List<Message> getMessages() throws IOException {
        return service.getMessages();
    }

    @RequestMapping("/messages/{id}")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public Address[] getMessage(@PathVariable("id") String messageId) throws IOException, MessagingException {
        return service.getMessage(messageId).getFrom();
    }


    @RequestMapping("/messages/{id}/category")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public String getMessageCategory(@PathVariable("id") String messageId) throws IOException, MessagingException {
        return service.getMessageCategory(messageId);
    }

    @RequestMapping("/messages/{id}/content")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public String getContent(@PathVariable("id") String messageId) throws IOException, MessagingException {
        return service.getMessageContent(messageId);
    }

    @RequestMapping("/messages/{id}/keywords")
    @ResponseBody
    @ResponseStatus(HttpStatus.FOUND)
    public List<String> getKeywords(@PathVariable("id") String messageId) throws IOException, MessagingException {
        return service.getKeywords(messageId);
    }

}
