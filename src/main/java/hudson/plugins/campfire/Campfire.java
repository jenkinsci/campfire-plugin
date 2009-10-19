package hudson.plugins.campfire;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlElement;

public class Campfire {
    private HttpClient client;
    private WebClient webClient;
    private String subdomain;
    private String email;
    private String password;

    public Campfire(String subdomain, String email, String password) {
        super();
        this.subdomain = subdomain;
        this.email = email;
        this.password = password;
        client = new HttpClient();
        client.getParams().setParameter("http.useragent", "JTinder");
        webClient = new WebClient();
        webClient.setWebConnection(new HttpClientBackedWebConnection(webClient, client));
        webClient.setJavaScriptEnabled(false);
        webClient.setCookiesEnabled(true);
    }

    public int post(String url, Map<String, String> params) throws IOException {
        PostMethod post = new PostMethod("http://" + this.subdomain + ".campfirenow.com/" + url);
        post.setRequestHeader("X-Requested-With", "XMLHttpRequest");
        post.setRequestHeader("X-Prototype-Version", "1.5.1.1");
        post.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        for (String key : params.keySet()) {
            post.setParameter(key, params.get(key));
        }
        try {
            return client.executeMethod(post);
        } finally {
            post.releaseConnection();
        }
    }

    public HtmlPage get(String url) throws IOException {
        return (HtmlPage) webClient.getPage("http://" + this.subdomain + ".campfirenow.com/" + url);
    }

    public boolean verify(int returnCode) {
        return (returnCode == 200 || (returnCode > 301 && returnCode < 399));
    }

    public void login() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("email_address", this.email);
        params.put("password", this.password);
        verify(post("login", params));
    }

    public void logout() throws IOException {
        get("logout");
    }

    private List<Room> getRooms() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        HtmlPage page = get("");
        List<Room> rooms = new ArrayList<Room>();
        for (HtmlElement div : (List<HtmlElement>) page.getByXPath("//div[contains(@class, 'room')]")) {
            rooms.add(new Room(this, ((HtmlElement) div.getByXPath(".//h2/a").get(0)).getTextContent().trim(), getRoomIdFromElement(div.getId())));
        }
        return rooms;
    }

    private String getRoomIdFromElement(String elementId) {
        String[] tokens = elementId.split("_");
        return tokens[tokens.length - 1].trim();
    }

    private Room findRoomByName(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        for (Room room : getRooms()) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }

    private Room createRoom(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("room[name]", name);
        params.put("room[topic]", "");
        verify(post("account/create/room?from=lobby", params));
        return findRoomByName(name);
    }

    public Room findOrCreateRoomByName(String name) throws IOException, ParserConfigurationException, XPathExpressionException, SAXException {
        Room room = findRoomByName(name);
        if (room != null) {
            return room;
        }
        return createRoom(name);
    }
}
