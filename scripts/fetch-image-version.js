const fetch = require("isomorphic-fetch");

return fetch('https://oss.cloudogu.com/jenkins/job/scm-manager-github/job/scm-manager/job/develop/api/json')
  .then(response => response.json())
  .then(json => json.lastSuccessfulBuild.number)
  .then(number => fetch(`https://oss.cloudogu.com/jenkins/job/scm-manager-github/job/scm-manager/job/develop/${number}/api/json`))
  .then(response => response.json())
  .then(json => {
    for (const action of json.actions) {
      if (action.lastBuiltRevision) {
        return {
          build: json.number,
          hash: action.lastBuiltRevision.SHA1
        }
      }
    }
    throw new Error("could not find action with lastBuiltRevision");
  })
  .then(data => {
    return fetch(`https://raw.githubusercontent.com/scm-manager/scm-manager/${data.hash}/gradle.properties`)
      .then(response => response.text())
      .then(text => {
        const lines = text.match(/[^\r\n]+/g);
        for (const line of lines) {
          if (line.startsWith("version")) {
            return line.split("=")[1].trim();
          }
        }
      }).then(version => {
        return {
          ...data,
          version
        }
      })
  })
  .then(data => {
    let tag = data.version.replace('-SNAPSHOT', '');
    tag += "-" + data.hash.substring(0, 9)
    tag += "-" + data.build;
    return tag;
  })
  .then(t => console.log(t));
