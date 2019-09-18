/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailapplication;

import com.sun.mail.pop3.POP3Folder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

/**
 *
 * @author deniz
 */
public class EmailManager {

    public void checkAccount(Account account) {
        try {

            // hesap bilgilerini bul
            Properties properties = new Properties();

            properties.put("mail.pop3.host", account.getIncomingServer());
            properties.put("mail.pop3.port", account.getIncomingPort());
            properties.put("mail.pop3.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            // pop3 alanı yaratır
            Store store = emailSession.getStore("pop3s");

            store.connect(account.getIncomingServer(), account.getEmail(), account.getPassword());

            // inbox'i al
            POP3Folder emailFolder = (POP3Folder) store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            // inbox'daki mesajları al
            Message[] messages = emailFolder.getMessages();
            System.out.println("Account: "  + account.getAccountName() + " messages.length---" + messages.length);

            FetchProfile fp = new FetchProfile();
            fp.add(UIDFolder.FetchProfileItem.UID);
            emailFolder.fetch(messages, fp);

            for (int i = 0, n = messages.length; i < n; i++) {

                Message message = messages[i];

                // eger bu mesaj veritabanında varsa bu mesajı atla.
                if (!checkUID(emailFolder.getUID(message))) {
                    continue;
                }

                // eger veritabanında yoksa bilgileri okuyup veri tabanına at
                Email email = new Email();
                email.setIncoming(true);
                email.setReadMark(false);
                email.setSubject(MimeUtility.decodeText(message.getSubject()));
                email.setUid(emailFolder.getUID(message));
                email.setRelatedAccount(account);
                
                email.setFromAddress(MimeUtility.decodeText(message.getFrom()[0].toString()));

                // TO kısmını oluştur
                Address[] a;
                String str = "";
                if ((a = message.getRecipients(Message.RecipientType.TO)) != null) {
                    for (int j = 0; j < a.length; j++) {
                        str += MimeUtility.decodeText(a[j].toString()) + ",";
                        InternetAddress ia = (InternetAddress) a[j];
                        if (ia.isGroup()) {
                            InternetAddress[] aa = ia.getGroup(false);
                            for (int k = 0; k < aa.length; k++) {
                                str += MimeUtility.decodeText(aa[k].toString()) + ",";
                            }
                        }
                    }
                }
                if (str.endsWith(",")) {
                    str = str.substring(0, str.length() - 1);
                }
                email.setToAddress(str);

                // içeriği al
                email.setContent(getText(message));
                email.setDate(message.getSentDate());

                // attachmentları kaydet
                List<File> files = saveAndGetAttachments(message);
                // attachment lists oluştur
                StringBuilder sb = new StringBuilder();
                if(files != null) {
                    for (File file : files) {
                        sb.append(file.getAbsolutePath()).append(',');                     
                    }
                }
                if(sb.length() > 0 && sb.charAt(sb.length()-1) == ',') {
                    sb.deleteCharAt(sb.length()-1); 
                }
                email.setAttachments(sb.toString());
                
                // veritabanına yaz.
                persist(email);

                System.out.println("---------------------------------");
                System.out.println("Email Number " + (i + 1));
                System.out.println("UID " + email.getUid());
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                System.out.println("Text: " + getText(message));

            }

            //close the store and folder objects
            emailFolder.close(false);
            store.close();

        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // http://www.oracle.com/technetwork/java/javamail/faq/index.html 
    // adresinden alınmıştır.
    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String) p.getContent();
            // textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null) {
                        text = getText(bp);
                    }
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null) {
                        return s;
                    }
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }

        return null;
    }

    public void persist(Object object) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("MailApplicationPU");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        try {
            em.persist(object);
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            em.getTransaction().rollback();
        } finally {
            em.close();
        }
    }

    public boolean checkUID(String uid) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("MailApplicationPU");
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT e FROM Email e WHERE e.uid = :uid");
        List<Email> list = query.setParameter("uid", uid).getResultList();
        return list.isEmpty();
    }

    void sendEmail(final Email email) {

        // ayarları ilgili hesaptan oku
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", email.getRelatedAccount().getOutgoingServer());
        props.put("mail.smtp.port", email.getRelatedAccount().getOutgoingPort());

        // kullanıcı adı ve şifre ayarı
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(email.getRelatedAccount().getEmail(),
                                email.getRelatedAccount().getPassword());
                    }
                });

        // emaili oluştur
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email.getRelatedAccount().getEmail()));

            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(email.getToAddress()));
            if(email.getCcAddress() != null) {
                message.setRecipients(Message.RecipientType.CC,
                    InternetAddress.parse(email.getCcAddress()));
            }
            
            if(email.getBccAddress() != null) {
                message.setRecipients(Message.RecipientType.BCC,
                    InternetAddress.parse(email.getBccAddress()));
            }
            
            message.setSubject(email.getSubject()== null?"":email.getSubject());
            
            BodyPart messageBodyPart = new MimeBodyPart();
            // email içeriğini ayarla
            messageBodyPart.setText(email.getContent());
            
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            
            // ek dosyalarını ekle
            String[] files = email.getAttachments().split(",");
            for (String file : files) {
                if(file != null && !file.isEmpty()) {
                    addAttachment(multipart, file);
                }
            } 
            
            // içeriği çoklu bölümlemeye ayarla
            message.setContent(multipart);
            
            // emaili yolla
            Transport.send(message);

            // yollanan epostayı veritabanına kayıt et.
            email.setIncoming(false);
            persist(email);
            
        } catch (MessagingException ex) {
            Logger.getLogger(EmailManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private static void addAttachment(Multipart multipart, String filename) throws MessagingException {
        DataSource source = new FileDataSource(filename);
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(new File(filename).getName());
        multipart.addBodyPart(messageBodyPart);
    }
    
    public List<File> saveAndGetAttachments(Message message) throws Exception {
    Object content = message.getContent();
    if (content instanceof String)
        return null;        

    if (content instanceof Multipart) {
        Multipart multipart = (Multipart) content;
        List<File> result = new ArrayList<File>();

        for (int i = 0; i < multipart.getCount(); i++) {
            result.addAll(getAttachments(multipart.getBodyPart(i)));
        }
        return result;

    }
    return null;
}

private List<File> getAttachments(BodyPart part) throws Exception {
    List<File> result = new ArrayList<File>();
    
    Object content = part.getContent();
    if (content instanceof InputStream || content instanceof String) {
        // attachment mı diye kontrol et
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) || (part.getFileName()!= null && !part.getFileName().isEmpty())) {
            
            // attachment'i dosyaya yaz
            InputStream is = part.getInputStream();
            File f = new File("c:/tmp/" + part.getFileName());
            f.createNewFile();
            // dosyayı kopyala
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buf = new byte[4096];
            int bytesRead;
            while((bytesRead = is.read(buf))!=-1) {
                fos.write(buf, 0, bytesRead);
            }
            fos.close();
            
            result.add(f);
            return result;
        } else {
            return new ArrayList<File>();
        }
    }

    if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                result.addAll(getAttachments(bodyPart));
            }
    }
    return result;
}
}
