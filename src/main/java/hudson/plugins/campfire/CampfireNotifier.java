package hudson.plugins.campfire;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

public class CampfireNotifier extends Notifier {

    private Campfire campfire;
    private Room room;
    private String hudsonUrl;
    private String notificationTemplate;
    private boolean smartNotify;
    private boolean sound;

    // getters for project configuration..
    // Configured room name / subdomain / token should be null unless different from descriptor/global values
    public String getConfiguredRoomName() {
        if ( DESCRIPTOR.getRoom().equals(room.getName()) ) {
            return null;
        } else {
            return room.getName();
        }
    }

    public String getConfiguredSubdomain() {
        if ( DESCRIPTOR.getSubdomain().equals(campfire.getSubdomain()) ) {
            return null;
        } else {
            return campfire.getSubdomain();
        }
    }

    public String getConfiguredToken() {
        if ( DESCRIPTOR.getToken().equals(campfire.getToken()) ) {
            return null;
        } else {
            return campfire.getToken();
        }
    }

    public String getConfiguredNotificationTemplate() {
        if ( DESCRIPTOR.getNotificationTemplate().equals(notificationTemplate) ) {
            return null;
        } else {
            return notificationTemplate;
        }
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private static final Logger LOGGER = Logger.getLogger(CampfireNotifier.class.getName());

    public CampfireNotifier() {
        super();
        initialize();
    }

    @DataBoundConstructor
    public CampfireNotifier(String subdomain, String token, String room, String hudsonUrl, String notificationTemplate,
                            boolean ssl, boolean smartNotify, boolean sound) {
        super();
        initialize(subdomain, token, room, hudsonUrl, notificationTemplate, ssl, smartNotify, sound);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    private String interpolate(String base, Map<String, String> context) {
        StringBuilder builder = new StringBuilder();
        int pos = 0;

        while (pos < base.length()) {
            int startIndex = base.indexOf("%", pos);
            if (startIndex >= 0) {
                builder.append(base.substring(pos, startIndex));

                int endIndex = base.indexOf("%", startIndex + 1);

                if (endIndex > 0) {
                    String key = base.substring(startIndex + 1, endIndex).trim();
                    if (key.length() > 0) {
                        String value = context.get(key);
                        if (value == null) {
                            value = "";
                        }
                        builder.append(value);
                    } else {
                        builder.append("%");
                    }
                    pos = endIndex + 1;
                } else {
                    // should error out here but quick and dirty for now
                    builder.append("%");
                    pos = startIndex + 1;
                }
            } else {
                builder.append(base.substring(pos));
                pos = base.length();
            }
        }
        return builder.toString();
    }

    private String computeChangeString(AbstractBuild<?, ?> build) {
        String changeString = "No changes";
        if (!build.hasChangeSetComputed()) {
            changeString = "Changes not determined";
        } else if (build.getChangeSet().iterator().hasNext()) {
            ChangeLogSet changeSet = build.getChangeSet();
            ChangeLogSet.Entry entry = build.getChangeSet().iterator().next();
            // note: iterator should return recent changes first, but GitChangeSetList currently reverses the log entries
            if (changeSet.getClass().getSimpleName().equals("GitChangeSetList")) {
                String exceptionLogMsg = "Workaround to obtain latest commit info from git plugin failed";
                try {
                    // find the sha for the first commit in the changelog file, and then grab the corresponding entry from the changeset, yikes!
                    String changeLogPath = build.getRootDir().toString() + File.separator + "changelog.xml";
                    String sha = getCommitHash(changeLogPath);
                    if (!"".equals(sha)) {
                        Method getIdMethod = entry.getClass().getDeclaredMethod("getId");
                        for(ChangeLogSet.Entry nextEntry : build.getChangeSet()) {
                            if ( ( (String)getIdMethod.invoke(entry) ).compareTo(sha) != 0 ) entry = nextEntry;
                        }
                    }
                } catch ( IOException e ){
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( NoSuchMethodException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( IllegalAccessException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( SecurityException e ) {
                    LOGGER.log(Level.WARNING, exceptionLogMsg, e);
                } catch ( Exception e ) {
                    throw new RuntimeException(e.getClass().getName() + ": " + e.getMessage(), e);
                }
            }
            String commitMsg = entry.getMsg().trim();
            if (!"".equals(commitMsg)) {
                if (commitMsg.length() > 47) {
                    commitMsg = commitMsg.substring(0, 46)  + "...";
                }
                changeString = commitMsg + " - " + entry.getAuthor().toString();
            }
        }
        return changeString;
    }

    private Map<String, String> buildContextFor(AbstractBuild<?, ?> build) {
        HashMap<String, String> context = new HashMap<String, String>();

        context.put("PROJECT_NAME", build.getProject().getName());
        context.put("PROJECT_DISPLAY_NAME", build.getProject().getDisplayName());
        context.put("PROJECT_FULL_NAME", build.getProject().getFullName());
        context.put("PROJECT_FULL_DISPLAY_NAME", build.getProject().getFullDisplayName());

        context.put("BUILD_DISPLAY_NAME", build.getDisplayName());

        Result result = build.getResult();
        String resultString = result.toString();
        context.put("RESULT", resultString);
        if (!smartNotify && result == Result.SUCCESS) {
            context.put("SMART_RESULT", resultString.toLowerCase());
        } else {
            context.put("SMART_RESULT", resultString);
        }

        context.put("CHANGES", computeChangeString(build));

        if (hudsonUrl != null && hudsonUrl.length() > 1) {
            context.put("BUILD_URL", hudsonUrl + build.getUrl());
        }

        return context;
    }

    private void publish(AbstractBuild<?, ?> build) throws IOException {
        checkCampfireConnection();

        if (notificationTemplate == null || notificationTemplate.trim().length() == 0) {
            return;
        }

        Map<String, String> context = buildContextFor(build);

        String message = interpolate(notificationTemplate, context);

        room.speak(message);

        if (sound) {
          String message_sound;
          if ("FAILURE".equals(build.getResult().toString())) {
            message_sound = "trombone";
          } else {
            message_sound = "rimshot";
          }
          room.play(message_sound);
        }
    }

    private String getCommitHash(String changeLogPath) throws IOException {
        String sha = "";
        BufferedReader reader = new BufferedReader(new FileReader(changeLogPath));
        String line;
        while((line = reader.readLine()) != null) {
            if (line.matches("^commit [a-zA-Z0-9]+$")) {
                sha = line.replace("commit ", "");
                break;
            }
        }
        reader.close();
        return sha;
    }

    private void checkCampfireConnection() {
        if (campfire == null) {
            initialize();
        }
    }

    private void initialize()  {
        initialize(DESCRIPTOR.getSubdomain(), DESCRIPTOR.getToken(), room.getName(), DESCRIPTOR.getHudsonUrl(),
            DESCRIPTOR.getNotificationTemplate(), DESCRIPTOR.getSsl(), DESCRIPTOR.getSmartNotify(),
            DESCRIPTOR.getSound());
    }

    private void initialize(String subdomain, String token, String roomName, String hudsonUrl, String notificationTemplate,
                            boolean ssl, boolean smartNotify, boolean sound) {
        campfire = new Campfire(subdomain, token, ssl);
        this.room = campfire.findRoomByName(roomName);
        if ( this.room == null ) {
            throw new RuntimeException("Room '" + roomName + "' not found - verify name and room permissions");
        }
        this.hudsonUrl = hudsonUrl;
        this.notificationTemplate = notificationTemplate;
        this.smartNotify = smartNotify;
        this.sound = sound;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        // If SmartNotify is enabled, only notify if:
        //  (1) there was no previous build, or
        //  (2) the current build did not succeed, or
        //  (3) the previous build failed and the current build succeeded.
        if (smartNotify) {
            AbstractBuild previousBuild = build.getPreviousBuild();
            if (previousBuild == null ||
                build.getResult() != Result.SUCCESS ||
                previousBuild.getResult() != Result.SUCCESS)
            {
                publish(build);
            }
        } else {
            publish(build);
        }
        return true;
    }
}
