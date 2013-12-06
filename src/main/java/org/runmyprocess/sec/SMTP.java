package org.runmyprocess.sec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.runmyprocess.sec.Config;
import org.runmyprocess.sec.ProtocolInterface;
import org.runmyprocess.sec.Response;
import org.runmyprocess.sec.SECErrorManager;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.runmyprocess.json.JSONArray;
import org.runmyprocess.json.JSONObject;


public class SMTP implements ProtocolInterface {
	
   
	private Response response = new Response();
	
	public SMTP() {
		// TODO Auto-generated constructor stub
	}
	public String decode(String s) {
	    return StringUtils.newStringUtf8(Base64.decodeBase64(s));
	}
	public String encode(String s) {
	    return Base64.encodeBase64String(StringUtils.getBytesUtf8(s));
	}
	private JSONObject SMTPError(String error){

		response.setStatus(500);//sets the return status to internal server error
		JSONObject errorObject = new JSONObject();
		errorObject.put("error", error.toString());
		return errorObject;
	}
	
	  /**
	   * Sends an e-mail message from a SMTP host with a list of attached files. 
	   * 
	   */
	  private static void sendEmailWithAttachment(Properties props,
	      final String userName, final String password, String fromAddress,String toAddress,
	      String subject, String message, JSONArray attachedFiles)
	          throws AddressException, MessagingException , Exception{
		
				// creates a new session with an authenticator
				Authenticator auth = new Authenticator() {
				  public PasswordAuthentication getPasswordAuthentication() {
				    return new PasswordAuthentication(userName, password);
				  }
				};
				Session session = Session.getInstance(props, auth);
				
				// creates a new e-mail message
				Message msg = new MimeMessage(session);
				
				msg.setFrom(new InternetAddress(fromAddress));
				//InternetAddress[] toAddresses = { new InternetAddress(toAddress) };
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
				msg.setSubject(subject);
				msg.setSentDate(new Date());
				
				// creates message part
				MimeBodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setContent(message, "text/html");
				
				// creates multi-part
				Multipart multipart = new MimeMultipart();
				multipart.addBodyPart(messageBodyPart);
				
				// adds attachments
				
				if (attachedFiles != null && attachedFiles.size() > 0) {
				      for (Object aObject : attachedFiles) {
						JSONObject aFile = JSONObject.fromObject(aObject);
						try {	
							 String btsStr=aFile.getString("data");
				
							 byte[] bts = Base64.decodeBase64(btsStr);
							 ByteArrayInputStream fis = new ByteArrayInputStream(bts);
						        ByteArrayOutputStream baos = new ByteArrayOutputStream();
						        byte[] buf = new byte[1024];
						        try {
						            for (int readNum; (readNum = fis.read(buf)) != -1;) {
						            	baos.write(buf, 0, readNum); //no doubt here is 0
						                //Writes len bytes from the specified byte array starting at offset off to this byte array output stream.
						            }
						        } catch (IOException e) {
						        	//ex.printStackTrace();
						        	throw e;
						        }
						        DataSource aAttachment = new  ByteArrayDataSource(baos.toByteArray(),"application/octet-stream");
						        messageBodyPart = new MimeBodyPart();
						        DataHandler dataHandler = new DataHandler(aAttachment);
						        messageBodyPart.setDataHandler(dataHandler); 
				
							messageBodyPart.setFileName(aFile.getString("name"));
							multipart.addBodyPart(messageBodyPart);
				
						} catch (Exception e) {
						 // e.printStackTrace();
						  throw e;
						}
				      }
				}
				
				// Put parts in message
				msg.setContent(multipart);   
				// sends the e-mail
				try{
					Transport.send(msg);
				}catch(Exception e){
					 //e.printStackTrace();
					 throw e;
				}
	  }
	
	 
	  @Override
	public void accept(JSONObject jsonObject,String configPath) {

		try {
			
			final String username = jsonObject.getString("username");
			final String password;
			
			JSONObject passObj = jsonObject.getJSONObject("password");
			String enc = passObj.getString("encoder").toUpperCase().replaceAll("\\s+","");
			if(enc.equals("BASE64")){
				password  = new String(Base64.decodeBase64(passObj.getString("password"))); 
			}else{
				password  = passObj.getString("password"); 
			}    

		      Properties props = new Properties();

		      System.out.println("Searching for SMTP config file ...");
			  Config config = new Config("configFiles"+File.separator+"SMTP.config",true);
            System.out.println("SMTP config file found");
		      
		      Set<?> keys = config.configFile.keySet();

			  for (Object key : keys) {
				  props.setProperty(key.toString(), config.getProperty(key.toString()));
			  }

			 String fromMail = jsonObject.getString("from");
			 String toMail =  jsonObject.getString("to");
			 String subject =  jsonObject.getString("subject");
			 String message =  jsonObject.getString("body");
			 JSONArray attachedFiles =  jsonObject.getJSONArray("attachedFiles");
			 
			try{ 
				System.out.println("Sending mail ...");
				sendEmailWithAttachment(props, username, password,fromMail, toMail, subject, message, attachedFiles);
				response.setStatus(200);
				JSONObject resp = new JSONObject();
				System.out.println("Mail Sent!");
				resp.put("Message", "Mail Sent!");
				response.setData(resp);
				
			} catch (Exception e) {
				response.setData(this.SMTPError(e.getMessage()));
	        	SECErrorManager errorManager = new SECErrorManager();
	        	errorManager.logError(e.getMessage(), Level.SEVERE);
				e.printStackTrace();
	        	throw e;
			}		
		} catch (Exception e) {
			response.setData(this.SMTPError(e.getMessage()));
        	SECErrorManager errorManager = new SECErrorManager();
        	errorManager.logError(e.getMessage(), Level.SEVERE);
			e.printStackTrace();
		}
	}
	
	@Override
	public Response getResponse() {
		return response;
	}
	
}
