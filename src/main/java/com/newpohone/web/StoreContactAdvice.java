package com.newpohone.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class StoreContactAdvice {

    @Value("${newphone.whatsapp.phone:573001001001}")
    private String whatsappPhone;

    @Value("${newphone.whatsapp.display:+57 300 100 1001}")
    private String whatsappDisplay;

    @Value("${newphone.whatsapp.message:Hola, quiero hablar con un asesor comercial de Newphone.}")
    private String whatsappMessage;

    @Value("${newphone.contact.email:asesor@newphone.com}")
    private String contactEmail;

    @Value("${newphone.contact.hours:Lunes a sábado, 8:00 a. m. a 6:00 p. m.}")
    private String contactHours;

    @ModelAttribute("storeContact")
    public Map<String, String> storeContact() {
        String encodedMessage = URLEncoder.encode(whatsappMessage, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String digits = whatsappPhone.replaceAll("\\D", "");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("phone", digits);
        contact.put("display", whatsappDisplay);
        contact.put("message", whatsappMessage);
        contact.put("whatsappUrl", "https://wa.me/" + digits + "?text=" + encodedMessage);
        contact.put("whatsappApiUrl", "https://api.whatsapp.com/send?phone=" + digits + "&text=" + encodedMessage);
        contact.put("telUrl", "tel:+" + digits);
        contact.put("email", contactEmail);
        contact.put("mailto", "mailto:" + contactEmail + "?subject="
                + URLEncoder.encode("Asesoría Newphone", StandardCharsets.UTF_8).replace("+", "%20"));
        contact.put("hours", contactHours);
        return contact;
    }
}
