package hudson.plugins.campfire;

import hudson.tasks.Publisher;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.AbstractProject;

import java.io.IOException;

import org.kohsuke.stapler.StaplerRequest;
import org.xml.sax.SAXException;
import net.sf.json.JSONObject;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private transient Campfire campfire;
    private boolean enabled = false;
    private String subdomain;
    private String email;
    private String password;
    private String room;

    public DescriptorImpl() {
        super(CampfireNotifier.class);
        load();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRoom() {
        return room;
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
        return true;
    }

    /**
     * @see hudson.model.Descriptor#newInstance(org.kohsuke.stapler.StaplerRequest)
     */
    @Override
    public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        CampfireNotifier result = new CampfireNotifier();
        subdomain = req.getParameter("subdomain");
        email = req.getParameter("email");
        password = req.getParameter("password");
        room = req.getParameter("room");
        try {
            initCampfire();
            result.room = campfire.findOrCreateRoomByName(room);
            result.room.join();
        } catch (IOException e) {
            throw new FormException(e, "Cannot join room");
        } catch (ParserConfigurationException e) {
            throw new FormException(e, "Cannot join room");
        } catch (XPathExpressionException e) {
            throw new FormException(e, "Cannot join room");
        } catch (SAXException e) {
            throw new FormException(e, "Cannot join room");
        }
        return result;
    }

    private void initCampfire() throws IOException {
        campfire = new Campfire(subdomain, email, password);
        campfire.login();
    }

    public void stop() throws IOException {
        if (campfire == null) {
            return;
        }
        campfire.logout();
        campfire = null;
    }

    /**
     * @see hudson.model.Descriptor#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return "Campfire Notification";
    }

    /**
     * @see hudson.model.Descriptor#getHelpFile()
     */
    @Override
    public String getHelpFile() {
        return "/plugin/campfire/help.html";
    }
}
