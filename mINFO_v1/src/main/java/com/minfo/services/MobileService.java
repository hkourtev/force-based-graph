package com.minfo.services;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.minfo.dao.hibernate.PoolHibernateDAO;
import com.minfo.mgr.PoolManager;
import com.minfo.mgr.UserManager;
import com.minfo.model.Answer;
import com.minfo.model.Pool;
import com.minfo.model.User;
import com.minfo.model.ws.WSPool;
import com.minfo.model.ws.WSUser;

//JDOM imports
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter; // Xalan-J 2 imports
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;

public class MobileService {
	private static transient Logger log = Logger
			.getLogger(MobileService.class);

	private transient PoolManager poolManager;
	private transient UserManager userManager;

	public void setPoolManager(PoolManager poolManager) {
		this.poolManager = poolManager;
	}

	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	public String getVersion() {
		log.debug("enter getVersion");
		return "MobileService v0.1";
	}

	public WSPool getPool(Long id) {
		log.debug("enter getPool");
		Pool p = poolManager.getPool(id);
		log.debug(p);

		WSPool wsPool = new WSPool();
		WSPool.fromPool(p, wsPool);
		return wsPool;
	}

	public WSUser registerNewUser() {
		User user = userManager.addRegisterUser();
		WSUser wsUser = new WSUser();
		WSUser.fromUser(user, wsUser);
		return wsUser;
	}

	public String makeAnswer(Long userId, String password, Long answerId) {
		// tu jakas weryfikacja bezpieczeństwa
		try {
			poolManager.makeAnswer(userId, answerId);
		} catch (Throwable t) {
			log.debug(t.getMessage(), t);
			return "ERR";
		}
		return "OK";
	}

	public String getNextPoolScreen(Long userId, String password) {
		Pool pool = poolManager.getNextPoolForUser(userId);
		
		return getPoolScreen(pool.getId());
	}
	
	public String getPoolScreen(Long id) {

		String xml = getPoolScreenXML(id);
		String transformed = null;
		log.debug("xml to tranform:" + xml);
		BufferedOutputStream bos = null;
		ByteArrayInputStream bais = null;
		try {
			InputStream xsltStream = this.getClass().getResourceAsStream("/com/minfo/services/xslt/template.xsl");
			log.debug("xsltStream="+xsltStream);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			bos = new BufferedOutputStream(bs);
			bais = new ByteArrayInputStream(xml.getBytes());
			transform(bais, xsltStream, bos);
			transformed = new String(bs.toByteArray());
		} finally {
			try {
				if (bos != null) {
					bos.close();
				}
				if (bais != null) {
					bais.close();
				}
			} catch (IOException e) {
				log.debug(e.getMessage(), e);
			}
		}

		log.debug("xml transformed:" + transformed);
		return transformed;
	}

	private void transform(InputStream document, InputStream xsltStream,
			OutputStream os) {
		try {
			// Set up the XSLT stylesheet for use with Xalan-J 2
			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();
			Templates stylesheet = transformerFactory
					.newTemplates(new StreamSource(xsltStream));
			Transformer processor = stylesheet.newTransformer();
			StreamSource source = new StreamSource(document);
			StreamResult result = new StreamResult(os);
			processor.transform(source, result);
		} catch (Throwable t) {
			log.debug(t.getMessage(), t);
		}
	}

	private String getPoolScreenXML(Long id) {
		log.debug("enter getPoolScreenXML");

		Pool p = poolManager.getPool(id);

		try {
			String mainElement = "pool";

			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			BufferedOutputStream bos = new BufferedOutputStream(bs);
			if (bos != null) {
				try {
					OutputFormat of = new OutputFormat("XML", "UTF-8", true);
					of.setIndent(1);
					of.setIndenting(true);

					XMLSerializer serializer = new XMLSerializer(bos, of);
					ContentHandler hd = serializer.asContentHandler();
					hd.startDocument();
					AttributesImpl atts = new AttributesImpl();
					atts.addAttribute("", "", "id", "CDATA", p.getId()
							.toString());
					hd.startElement("", "", mainElement, atts);
					atts.clear();
					atts.addAttribute("", "", "type", "xs:string", "single");
					hd.startElement("", "", "question", atts);
					hd.characters(p.getQuestion().toCharArray(), 0, p
							.getQuestion().length());
					hd.endElement("", "", "question");

					hd.startElement("", "", "answers", null);
					for (Answer a : p.getAnswers()) {
						atts.clear();
						atts.addAttribute("", "", "id", "CDATA", a.getId()
								.toString());
						hd.startElement("", "", "answer", atts);
						hd.characters(a.getAnswer().toCharArray(), 0, a
								.getAnswer().length());
						hd.endElement("", "", "answer");
					}
					hd.endElement("", "", "answers");

					hd.endElement("", "", mainElement);
					hd.endDocument();
					return new String(bs.toByteArray());
				} finally {
					if (bos != null) {
						bos.close();
					}
				}
			}

		} catch (Throwable t) {
			log.error(t.getMessage(), t);
		}
		return null;
	}
}
