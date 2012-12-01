## Campfire notifier plugin for Jenkins

This is a fork of the plugin developed by Jens Lukowski. More information about
the original plugin is available from the [Hudson
wiki](http://wiki.hudson-ci.org/display/HUDSON/Campfire+Plugin) and [this blog
post from
Jens](http://schneide.wordpress.com/2009/10/26/a-campfire-plugin-for-hudson/).

Development of the original plugin seemed to stall, so I forked it to address a
number of issues and add some extra features...

* Refactored the code to fix a number of null pointer exceptions.
* Moved from per-job to global config.
* Fixed issues with configuration details being lost after a restart.
* Tidied up jelly view for configuration form and added help files for each
  field.
* Added a link to the build in notifications sent to campfire.

Other features have since been added including...

* Support for campfire accounts with SSL enabled, added by Joshua Krall.
* A smart notification feature, added by Brad Greenlee, which disables
  success notifications unless the previous build was unsuccessful.
* Play sounds on build success and failure, added by Henry Poydar.
* Build notifications now include commit info.
* Room to which notifications are sent can be customised per-project.
* Subdomain and API token can be customized per-project.

Note: The plugin code is a bit of a mess, partly just because I don't have a
lot of Java experience, but also because I simply haven't got the time to tidy
it up. It does work though, and we use it daily without any trouble.

Update: The plugin code is less of a mess that it was, thanks to Dante Briones.
Yay, thanks Dante, and welcome aboard!

### Installation

This plugin is available from the [Update Center]
(https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins)
in your Jenkins installation.

### Troubleshooting

If you get HttpClient or WebClient exceptions, that probably means you've got
some configuration setting wrong (while there is some validation of
configuration settings, it's far from extensive).
