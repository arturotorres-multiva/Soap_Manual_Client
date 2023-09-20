package mx.com.multiva;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import sun.misc.BASE64Encoder;
import com.sun.xml.internal.messaging.saaj.util.Base64;

public class ManualSoapClient {

	 public String base64Decode(String str) {
	        return new String(Base64.base64Decode(str));
	    }
	 
	 public String base64Encode(String str) {
	        return new String(Base64.encode(str.getBytes()));
	    }
	 
	public static void main( String[] args ) {
		new ManualSoapClient().callWebService();		
	}
		
    private static final String USERNAME = "T0ZT1VVNFUg==";
    private static final String PASSWORD = "QWIx1MjM0NTY=";
	private static final String url = "http://10.160.245.50:8083/T24.TWSS.CUSTOMER/services";
	
	public void callWebService() {
		
		/* Ejemplo
		<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:t24="http://temenos.com/T24.TWS.CUSTOMER.SECURE2">
		   <soapenv:Header/>
		   <soapenv:Body>
		      <t24:WSCUSEID>
		         <CompanyCode>?</CompanyCode>
		         <BMVEVPMIDTRANSType>
		            <enquiryInputCollection>
		               <columnName>?</columnName>
		               <criteriaValue>?</criteriaValue>
		               <operand>?</operand>
		            </enquiryInputCollection>
		         </BMVEVPMIDTRANSType>
		      </t24:WSCUSEID>
		   </soapenv:Body>
		</soapenv:Envelope>
		*/

		try {
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			SOAPMessage soapResponse = soapConnection.call( createSOAPRequest(), url );

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			soapResponse.writeTo(out);
			String strMsg = new String( out.toByteArray() );
			System.out.println( "Response SOAP Message = \n" + strMsg );

			soapConnection.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private SOAPMessage createSOAPRequest() throws Exception {
		//Password_Digest = Base64 ( SHA-1 ( nonce + created + password ) )
		SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
		rand.setSeed(System.currentTimeMillis());
		byte[] nonceBytes = new byte[16];
		rand.nextBytes(nonceBytes);

		//Time
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		String createdDate = df.format(Calendar.getInstance().getTime());
		byte[] createdDateBytes = createdDate.getBytes("UTF-8");

		//Password
		byte[] passwordBytes = PASSWORD.getBytes("UTF-8");

		//SHA-1 hash.
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(nonceBytes);
		baos.write(createdDateBytes);
		baos.write(passwordBytes);
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		//		byte[] digestedPassword = md.digest(baos.toByteArray());

		//Password y Nonce
		//		String passwordB64 = (new BASE64Encoder()).encode(digestedPassword);
		String nonceB64 = (new BASE64Encoder()).encode(nonceBytes);
		
		//Request manual  ************************************************************************
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration("t24", "http://temenos.com/T24.TWSS.CUSTOMER");

		SOAPHeader header = envelope.getHeader();

		SOAPElement security = header.addChildElement("Security", "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
		security.addNamespaceDeclaration("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
		SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");
		UUID uuid = UUID.randomUUID();
		String uuidAsString = uuid.toString();
		usernameToken.addAttribute(new QName("wsu:Id"), "UsernameToken-" + uuidAsString);

		SOAPElement username = usernameToken.addChildElement("Username", "wsse");
		username.addTextNode(USERNAME); //username.addTextNode(base64Encode(USERNAME));

		SOAPElement password = usernameToken.addChildElement("Password", "wsse");
		password.setAttribute("Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
		//		        password.addTextNode(passwordB64); pass digest
		password.addTextNode(PASSWORD);//password.addTextNode(base64Encode(PASSWORD));

		SOAPElement nonce = usernameToken.addChildElement("Nonce", "wsse");
		nonce.setAttribute("EncodingType", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
		nonce.addTextNode(nonceB64);

		SOAPElement created = usernameToken.addChildElement("Created", "wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
		created.addTextNode(createdDate);

		SOAPBody soapBody1 = envelope.getBody();
		SOAPElement soapBodyElem = soapBody1.addChildElement("WSCUSEID", "t24");

		//				addElementAndValue(soapBodyElem, "CompanyCode", companyCode);

		SOAPBody soapBody2 = envelope.getBody();
		SOAPElement soapBodyElem2 = soapBody2.addChildElement("BMVEVPMIDTRANSType");

		SOAPBody soapBody3 = envelope.getBody();
		SOAPElement soapBodyElem3 = soapBody3.addChildElement("enquiryInputCollection");

		soapBodyElem.addChildElement( soapBodyElem2 );
		soapBodyElem2.addChildElement( soapBodyElem3 );

		//				addElementAndValue(soapBodyElem3, "ColumnName", columnName);
		//				addElementAndValue(soapBodyElem3, "CriteriaValue", criteriaValue);
		//				addElementAndValue(soapBodyElem3, "Operand", operand);

		soapMessage.saveChanges();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		soapMessage.writeTo(out);
		String strMsg = new String(out.toByteArray());

		System.out.println( "Request SOAP Message = \n" + strMsg );

		return soapMessage;
	}
}
