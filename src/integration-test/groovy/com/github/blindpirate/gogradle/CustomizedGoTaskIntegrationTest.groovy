package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.crossplatform.Os
import com.github.blindpirate.gogradle.support.IntegrationTestSupport
import com.github.blindpirate.gogradle.support.WithResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GogradleRunner)
@WithResource('')
class CustomizedGoTaskIntegrationTest extends IntegrationTestSupport {
    String lsCmd = Os.getHostOs() == Os.WINDOWS ? 'cmd /C dir' : 'ls'

    @Before
    void setUp() {
        String buildDotGradle = """
${buildDotGradleBase}

golang {
    packagePath='a/b/c'
}

task ls(type: com.github.blindpirate.gogradle.Go){
    doLast {
        go 'version'
        run '${lsCmd}'
    }
}
"""
        writeBuildAndSettingsDotGradle(buildDotGradle)
    }

    @Test
    void 'customized go task should succeed'() {
        try {
            newBuild {
                it.forTasks('ls')
            }
        } finally {
            println(stderr)
            assert stdout.toString().contains('go version')
            assert stdout.toString().contains('settings.gradle')
        }
    }

    @Override
    File getProjectRoot() {
        return resource
    }
}
