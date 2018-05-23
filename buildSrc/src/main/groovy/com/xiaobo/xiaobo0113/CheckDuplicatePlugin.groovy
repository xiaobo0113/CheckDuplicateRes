package com.xiaobo.xiaobo0113

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.lang.reflect.Field

class CheckDuplicatePlugin implements Plugin<Project> {

    boolean mApplied = false

    @Override
    void apply(Project project) {
        if (mApplied) {
            log('already applied, just return.')
            return
        }

        mApplied = true
        init(project)
    }

    void init(Project project) {
        // 1. get the sdk directory based on the `local.properties` file
        String sdkDir = null
        def localProperties = new File(project.rootProject.rootDir, "local.properties")
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream { inputStream ->
                properties.load(inputStream)
            }
            sdkDir = properties.getProperty('sdk.dir')
        }

        Map<String, String> packageNames = new HashMap<>()
        if (null != sdkDir) {
            project.rootProject.subprojects { subproject ->
                subproject.afterEvaluate {
                    // 2. get the `android` extension of each project
                    def android = subproject.extensions.findByName('android')

                    // 3. get buildToolsVersion
                    String buildToolsVersion = android.buildToolsVersion

                    // 4. get compileSdkVersion
                    String compileSdkVersion = android.compileSdkVersion

                    // 5. get res dir
                    String resDir = android.sourceSets.main.res.srcDirs

                    // 6. get AndroidManifest.xml path
                    String manifestPath = android.sourceSets.main.manifest.srcFile

                    // 7. generate dir to save R.java
                    File destDir = new File(subproject.rootProject.buildDir, subproject.name)
                    if (!destDir.exists()) {
                        destDir.mkdirs()
                    }

                    // 8. call aapt to generate R.java:
                    // aapt package -m -J <R.java dir> -S <res dir> -I <android.jar dir> -M <AndroidManifest.xml dir>
                    def command = sdkDir + "/build-tools/" + buildToolsVersion + "/aapt " + " package -m " +
                            " -J " + destDir.absolutePath +
                            " -S " + resDir.replace('[', '').replace(']', '') +
                            " -I " + sdkDir + "/platforms/" + compileSdkVersion + "/android.jar" +
                            " -M " + manifestPath
                    command.execute().text.trim()

                    // 9. get module package name from its AndroidManifest.xml file
                    XmlSlurper parser = new XmlSlurper()
                    GPathResult result = parser.parse(new File(manifestPath))
                    String packageName = result['@package']
                    packageNames.put(subproject.name, packageName)

                    // 10. begin analyse R.java files
                    if (packageNames.size() == rootProject.subprojects.size()) {
                        checkDuplicate(subproject, packageNames)
                    }
                }
            }
        } else {
            log('can not get sdk directory, please specify sdk.di=xxx in `local.properties` file.')
        }
    }

    def checkDuplicate(Project project, Map<String, String> packageNames) {
        Map<String, String> rFiles = new HashMap<>()
        project.rootProject.subprojects { subproject ->
            String packageName = packageNames.get(subproject.name)
            File rFile = new File(rootProject.buildDir, subproject.name + '/' + packageName.replace('.', '/') + '/R.java')
            if (rFile.exists()) {
                // key is package name, value is the path of R.java
                rFiles.put(subproject.name, rFile.absolutePath)
            }
        }

        // get all res ids: key is id name, value is a list of module name where the module contains the res id
        Map<String, List<String>> rIds = new HashMap<>()
        rFiles.forEach { key, value ->
            // make R.java a class file, then reflection can be used
            def file = new File(value)
            def gcl = new GroovyClassLoader()
            String text = file.getText("utf-8")
            def prefix = text.findAll('''=\\s+\\{''')
            for (int i = 0; i < prefix.size(); i++) {
                text = text.replaceFirst('= \\{', '= [')
                text = text.replaceFirst('};', ']')
            }
            text = text.replace('public static', '')

            Class clazz = gcl.parseClass(text)
            Class[] innerClazz = clazz.getDeclaredClasses()
            if (innerClazz != null) {
                for (Class inner : innerClazz) {
                    Field[] fields = inner.getDeclaredFields()
                    for (Field field : fields) {
                        String[] strs = field.toString().split('R\\$')
                        String id = 'R$' + strs[1]
                        // exclude some special fields
                        if (id.findAll('\\$').size() > 1 || id.endsWith('.metaClass')) {
                            continue
                        }
                        if (rIds.containsKey(id)) {
                            List<String> module = rIds.get(id)
                            module.add(key)
                        } else {
                            List<String> module = new ArrayList<>();
                            module.add(key)
                            rIds.put(id, module)
                        }
                    }
                }
            }
        }

        log("==================================================>>>")
        log("============ below are duplicate resources =======>>>")
        log("==================================================>>>")
        rIds.sort().forEach { key, value ->
            if (null != value && value.size() >= 2) {
                println key + ", " + value.size() + ":\n" + value + "\n"
            }
        }
        println "total size: " + rIds.size()
        log("<<<==================================================")
        log("<<<========= above are duplicate resources =========")
        log("<<<==================================================")
    }

    void log(def l) {
        log('CheckDuplicatePlugin', l)
    }

    void log(String tag, def l) {
        println(tag + ': ' + l)
    }

}
