package com.serverproject.service;

public interface SmsSender {

    // toE164 hamesha "+91..." jaise full format mein aata hai
    void send(String toE164, String text) throws Exception;
}