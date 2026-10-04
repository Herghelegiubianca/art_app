package org.example.service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class EmailService {
    private static final String SMTP_EMAIL = System.getenv("SMTP_EMAIL");
    private static final String SMTP_PASSWORD = System.getenv("SMTP_PASSWORD");

    public void sendPasswordResetEmail(String toAddress, String token) {
        if (SMTP_EMAIL == null || SMTP_PASSWORD == null) {
            System.err.println("Eroare: Setează variabilele de mediu SMTP_EMAIL și SMTP_PASSWORD!");
            return;
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_EMAIL, SMTP_PASSWORD);
            }
        });

        CompletableFuture.runAsync(() -> {
            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SMTP_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
                message.setSubject("Resetare parolă - Aplicație Artă");


                String link = "http://localhost:8080/reset-password.html?token=" + token;

                String htmlContent = "<h3>Salut,</h3>"
                        + "<p>Ai cerut resetarea parolei pentru contul tău.</p>"
                        + "<p>Dă click pe link-ul de mai jos pentru a alege o parolă nouă (link-ul este valabil 15 minute):</p>"
                        + "<a href=\"" + link + "\" style=\"background-color:#4CAF50; color:white; padding:10px 20px; text-decoration:none; border-radius:5px; display:inline-block;\">Resetează Parola</a>"
                        + "<br><br><p>Dacă butonul nu funcționează, copiază acest link în browser: <br>" + link + "</p>"
                        + "<p>Dacă nu ai cerut tu asta, poți ignora în siguranță acest mesaj.</p>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);

            } catch (MessagingException e) {
                System.err.println("Eroare la trimiterea email-ului: " + e.getMessage());
            }
        });
    }
}