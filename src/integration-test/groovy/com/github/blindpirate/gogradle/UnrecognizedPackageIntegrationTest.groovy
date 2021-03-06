package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.support.*
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.StringUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GogradleRunner)
@WithResource('')
@WithIsolatedUserhome
@WithMockGo
class UnrecognizedPackageIntegrationTest extends IntegrationTestSupport {

    String unrecognized1Go = """
package main
import (
"unrecognized2"
"unrecognized2/sub"
)
"""
    String unrecognized2Go = """
package main
import (
"unrecognized3"
"unrecognized3/sub"
"unrecognized3/sub/sub"
)
"""

    @Before
    void setUp() {
        IOUtils.mkdir(resource, 'project')
        IOUtils.mkdir(resource, 'unrecognized1')
        IOUtils.mkdir(resource, 'unrecognized2')

        IOUtils.write(resource, 'unrecognized1/main.go', unrecognized1Go)
        IOUtils.write(resource, 'unrecognized2/main.go', unrecognized2Go)
    }

    String getResourceDir(String dirName) {
        return StringUtils.toUnixString(new File(resource, dirName))
    }

    @Test
    void 'build should succeed if unrecognized package is excluded'() {
        writeBuildAndSettingsDotGradle("""
System.setProperty('gradle.user.home','${StringUtils.toUnixString(userhome)}')
buildscript {
    dependencies {
        classpath files(new File(rootDir, '../../../libs/gogradle-${GogradleGlobal.GOGRADLE_VERSION}-all.jar'))
    }
}
apply plugin: 'com.github.blindpirate.gogradle'
golang {
    packagePath='my/project'
    goExecutable='${StringUtils.toUnixString(goBinPath)}'
}
dependencies {
    golang {
        build (name:'unrecognized1', dir: '${getResourceDir("unrecognized1")}'){
            exclude name:'unrecognized2'
       }
    }
}
""")
        newBuild {
            it.forTasks('resolveBuildDependencies')
        }

    }

    @Test
    @WithGitRepo(repoName = 'helloworld', fileName = 'helloworld.go')
    void 'build should succeed if url of unrecognized package is provided'() {
        writeBuildAndSettingsDotGradle("""
System.setProperty('gradle.user.home','${StringUtils.toUnixString(userhome)}')
buildscript {
    dependencies {
        classpath files(new File(rootDir, '../../../libs/gogradle-${GogradleGlobal.GOGRADLE_VERSION}-all.jar'))
    }
}
apply plugin: 'com.github.blindpirate.gogradle'
golang {
    packagePath='my/project'
    goExecutable='${StringUtils.toUnixString(goBinPath)}'
}

repositories {
    golang {
        root 'unrecognized2'
        url 'http://localhost:8080/helloworld'
    }
}

dependencies {
    golang {
        build (name:'unrecognized1', dir: '${getResourceDir("unrecognized1")}') 
    }
}

""")
        newBuild {
            it.forTasks('installBuildDependencies')
        }
        assert new File(resource, 'project/.gogradle/build_gopath/src/unrecognized2/helloworld.go').exists()
    }

    @Test
    void 'build should succeed if unrecognized package is provided as dir'() {
        writeBuildAndSettingsDotGradle("""
System.setProperty('gradle.user.home','${StringUtils.toUnixString(userhome)}')
buildscript {
    dependencies {
        classpath files(new File(rootDir, '../../../libs/gogradle-${GogradleGlobal.GOGRADLE_VERSION}-all.jar'))
    }
}
apply plugin: 'com.github.blindpirate.gogradle'
golang {
    packagePath='my/project'
    goExecutable='${StringUtils.toUnixString(goBinPath)}'
}

repositories {
    golang {
        root 'unrecognized2'
        dir '${getResourceDir("unrecognized2")}'
    }
}

dependencies {
    golang {
        build (name:'unrecognized1', dir: '${getResourceDir("unrecognized1")}'){
            exclude name:'unrecognized3'
        }
    }
}

""")
        newBuild {
            it.forTasks('resolveBuildDependencies')
        }
    }

    @Override
    File getProjectRoot() {
        return new File(resource, 'project')
    }

}
