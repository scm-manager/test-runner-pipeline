// this script installs required plugins for scm-manager

import sonia.scm.plugin.PluginManager
import sonia.scm.config.ScmConfiguration
import sonia.scm.util.ScmConfigurationUtil

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
]

def plugins = []

// methods

def scmConfig = injector.getInstance(ScmConfiguration.class);
scmConfig.setPluginUrl "https://oss.cloudogu.com/jenkins/job/scm-manager-github/job/ci-plugin-snapshot/job/master/lastSuccessfulBuild/artifact/plugins/plugin-center.json"
ScmConfigurationUtil.getInstance().store scmConfig
println "Set plugin center url to " + scmConfig.getPluginUrl

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

String pluginsParameter = System.getenv("TRP_PLUGINS")

if (pluginsParameter != null && !pluginsParameter.trim().isEmpty()) {
  def customPlugins = pluginsParameter.split(",")
  for (def plugin : customPlugins) {
    System.out.println("Add plugin to install queue: " + plugin.trim());
    plugins.add(plugin.trim())
  }
} else {
  println "Add all default plugins to install queue"
  plugins.addAll(defaultPlugins)
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
      restart |= installPlugin(pluginManager, name)
    }
  } else {
    System.out.println("plugin ${name} already installed.");
  }
}

if (restart) {
  System.out.println("restarting scm-manager");
  pluginManager.executePendingAndRestart();
} else {
  System.out.println("no new plugins installed");
}
