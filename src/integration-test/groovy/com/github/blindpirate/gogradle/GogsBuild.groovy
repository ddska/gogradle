package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.crossplatform.Arch
import com.github.blindpirate.gogradle.crossplatform.Os
import com.github.blindpirate.gogradle.support.AccessWeb
import com.github.blindpirate.gogradle.support.IntegrationTestSupport
import com.github.blindpirate.gogradle.support.OnlyWhen
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.ProcessUtils
import org.junit.Test
import org.junit.runner.RunWith

import java.nio.file.Path

@RunWith(GogradleRunner)
@OnlyWhen("System.getenv('GOGS_DIR')!=null&&'git version'.execute()")
class GogsBuild extends IntegrationTestSupport {
    File resource = new File(System.getenv('GOGS_DIR'))

    ProcessUtils processUtils = new ProcessUtils()

    String buildDotGradle = """
buildscript {
    dependencies {
        classpath files("${System.getProperty('GOGRADLE_ROOT')}/build/libs/gogradle-${GogradleGlobal.GOGRADLE_VERSION}-all.jar")
    }
}
apply plugin: 'com.github.blindpirate.gogradle'

golang {
    packagePath="github.com/gogits/gogs"
    goVersion='1.8'
}

goVet {
    continueWhenFail = true
}

"""

    @Test
    @AccessWeb
    void 'gogs should be built successfully'() {
        // v0.9.113
        assert processUtils.run(['git', 'checkout', '114c179e5a50e3313f7a5894100693805e64e440'], null, resource).waitFor() == 0

        // I don't know why it will fail on Windows
        if (Os.getHostOs() == Os.WINDOWS) {
            writeBuildAndSettingsDotGradle(buildDotGradle + 'goTest.enabled = false')
        } else {
            writeBuildAndSettingsDotGradle(buildDotGradle)
        }

        firstBuild()

        File firstBuildResult = getOutputExecutable()
//        long lastModified = firstBuildResult.lastModified()
//        String md5 = DigestUtils.md5Hex(new FileInputStream(firstBuildResult))
        assert processUtils.getStdout(processUtils.run(firstBuildResult.absolutePath)).contains('Gogs')
        assert new File(resource, 'gogradle.lock').exists()

        secondBuild()

        File secondBuildResult = getOutputExecutable()
        assert processUtils.getStdout(processUtils.run(secondBuildResult.absolutePath)).contains('Gogs')
//        assert secondBuildResult.lastModified() > lastModified
//        assert DigestUtils.md5Hex(new FileInputStream(secondBuildResult)) == md5
    }

    void firstBuild() {
        try {
            newBuild {
                it.forTasks('goBuild', 'goCheck', 'goLock', 'goDependencies')
            }
        } catch (Exception e) {
            throw e
        } finally {
            println(stdout)
            println(stderr)
        }
    }

    void secondBuild() {
        try {
            newBuild {
                it.forTasks('goBuild', 'goCheck', 'goDependencies')
            }
        } catch (Exception e) {
            throw e
        } finally {
            println(stdout)
            println(stderr)
        }
    }

    File getOutputExecutable() {
        Path gogsBinPath = resource.toPath().resolve(".gogradle/${Os.getHostOs()}_${Arch.getHostArch()}_gogs")
        if (Os.getHostOs() == Os.WINDOWS) {
            gogsBinPath.renameTo(gogsBinPath.toString() + '.exe')
        }
        IOUtils.chmodAddX(gogsBinPath)
        return gogsBinPath.toFile()
    }


    @Override
    File getProjectRoot() {
        return resource
    }
}
