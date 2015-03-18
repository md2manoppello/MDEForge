package org.mdeforge.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.mdeforge.business.model.EcoreMetamodel;
import org.mdeforge.business.model.GridFileMedia;
import org.mdeforge.business.model.Property;

public class Importer extends TestCase {

//	@Test
//	public void testEcoreMetamodel() throws Exception {
//		MDEForgeClient c = new MDEForgeClient("http://localhost:8080/mdeforge/", "test123", "test123");
//		EcoreMetamodel p = c.getEcoreMetamodel("54b3fe567d8444a8001e736d");
//		System.out.println("1" + p.getFile().getContent());
//		System.out.println("2__" + new String(p.getFile().getByteArray()));
//		assertNotNull(p);
//	}
	
	@Test
	public void test() {
		
		Document doc;
		String projectUrl = "http://www.emn.fr/z-info/atlanmod/index.php/Ecore";
		try {
			doc = Jsoup.connect(projectUrl).timeout(10000).get();
			Element e = doc.getElementById("content");
			Elements es = e.getAllElements();
			EcoreMetamodel emm = null;
			boolean enable = false;
			boolean complete = false;
			int count = 0;
			for (Element element : es) {
				if (element.tagName().equals("h3") && !enable)
				{
					if (count!=0)
					{
						enable = true;
						emm = new EcoreMetamodel();
						emm.setOpen(true);
						emm.setName(element.getElementsByTag("span").text());
					}
					count++;
				}
				else if(enable) {
					if (element.tagName().equals("p")) {
						String[] prop = element.text().split(":");
						if(prop.length == 2) {
							Property p = new Property();
							p.setName(prop[0].replace(" ", "").replace("", ""));
							p.setValue(prop[1]);
							emm.getProperties().add(p);
						}
					}
					if (element.tagName().equals("li") ) {
						String url = element.getElementsByTag("a").attr("href");
						System.out.println(url);
						
						URL url2 = new URL(url);
						URLConnection con = url2.openConnection();
						InputStream in = con.getInputStream();
						String encoding = con.getContentEncoding();
						encoding = encoding == null ? "UTF-8" : encoding;
						String body = IOUtils.toString(in, encoding);
						System.out.println(body);
						String file = body;
						System.out.println("Prova" + file);
						GridFileMedia gfm = new GridFileMedia();
						gfm.setContent(new String(Base64.encodeBase64(file.getBytes())));
						emm.setFile(gfm);
						enable = false;
						complete = true;
					}
				}
				if(complete) {
					try {
						complete = false;
						MDEForgeClient c = new MDEForgeClient("http://localhost:8080/mdeforge/", "test123", "test123");
						c.addEcoreMetamodel(emm);
					} catch (Exception e1) {
						System.err.println(emm.getName());
					}
				}
				
			}
			System.out.println("count: " + count);
			
		} catch (IOException e1) {
			System.out.println("azz");
			// TODO Auto-generated catch block
			System.out.println("Unable to connect at " + projectUrl + "Importer exception:" + e1.getMessage());
		}
		
		
	}

}
