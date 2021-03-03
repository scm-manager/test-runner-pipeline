// this script installs required plugins for scm-manager

import sonia.scm.plugin.PluginManager;
import groovy.json.JsonSlurper;

// default plugin configuration
def defaultPlugins = [
  "scm-gravatar-plugin",
  "scm-mail-plugin",
  "scm-review-plugin",
  "scm-tagprotection-plugin",
  "scm-jira-plugin",
  "scm-activity-plugin",
  "scm-statistic-plugin",
  "scm-pathwp-plugin",
  "scm-branchwp-plugin",
  "scm-notify-plugin",
  "scm-authormapping-plugin",
  "scm-groupmanager-plugin",
  "scm-pushlog-plugin",
  "scm-support-plugin",
  "scm-directfilelink-plugin",
  "scm-readme-plugin",
  "scm-editor-plugin",
  "scm-landingpage-plugin",
  "scm-repository-template-plugin",
  "scm-commit-message-checker-plugin",
  "scm-trace-monitor-plugin",
  "scm-markdown-plantuml-plugin"
];

def plugins = []
def pluginsFromOldInstallation = []

// methods

static def isInstalled(installed, name) {
  for (def plugin : installed) {
    if (plugin.descriptor.information.name.equals(name)) {
      return true;
    }
  }
  return false;
}

static def getAvailablePlugin(available, name) {
  for (def plugin : available) {
    if (plugin.descriptor.information.name.equals(name)) {
      return plugin.descriptor.information;
    }
  }
  return null;
}

static def installPlugin(pluginManager, pluginName) {
  try {
    pluginManager.install(pluginName, false);
    return true;
  } catch (sonia.scm.NotFoundException e) {
    System.out.println("plugin or dependency for ${pluginName} not found: ${e.message}");
  } catch (sonia.scm.plugin.PluginChecksumMismatchException e) {
    System.out.println("Plugin ${pluginName} had wrong checksum: ${e.message}");
  }
  return false;
}


plugins.addAll(defaultPlugins)

File pluginListFile = new File(sonia.scm.SCMContext.getContext().getBaseDirectory(), "installed_plugins_before_update.lst")
if (pluginListFile.exists()) {
  def reader = pluginListFile.newReader()
  def line
  while ((line = reader.readLine()) != null) {
    System.out.println("Add previously installed plugin '${line}'");
    plugins.add(line)
    pluginsFromOldInstallation.add(line)
  }
}

def pluginManager = injector.getInstance(PluginManager.class);
def available = pluginManager.getAvailable();
def installed = pluginManager.getInstalled();

def restart = false;
for (def name : plugins) {
  if (!isInstalled(installed, name)) {
    def availableInformation = getAvailablePlugin(available, name);
    if (availableInformation == null) {
      System.out.println("Cannot install missing plugin ${name}. No available plugin found!");
    } else {
      System.out.println("install missing plugin ${availableInformation.name} in version ${availableInformation.version}");
      pluginsFromOldInstallation.remove(name)
      restart |= installPlugin(pluginManager, name)
    }
  } else {
    pluginsFromOldInstallation.remove(name)
    System.out.println("plugin ${name} already installed.");
  }
}

if (pluginListFile.exists()) {
  if (pluginsFromOldInstallation.isEmpty()) {
    println "Deleting file with plugins from old installation; all plugins have been installed again."
    pluginListFile.delete()
  } else {
    println "Not all plugins from old installation could be installed; keeping list to try again next time."
  }
}

if (restart) {
  System.out.println("restarting scm-manager");
  pluginManager.executePendingAndRestart();
} else {
  System.out.println("no new plugins installed");
}