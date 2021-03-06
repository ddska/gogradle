package com.github.blindpirate.gogradle

import com.github.blindpirate.gogradle.support.*
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.StringUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GogradleRunner)
@WithMockGo
@WithResource('')
@WithIsolatedUserhome
class DependencyTestWithMockGit extends IntegrationTestSupport {
    File resource

    File projectRoot

    File localDependencyRoot

    File repositories

    @Before
    void setUp() {
        localDependencyRoot = new File(resource, 'localDependency')
        IOUtils.write(localDependencyRoot, 'main.go', '')
        IOUtils.write(localDependencyRoot, 'vendor/unrecognized/main.go', '')

        projectRoot = new File(resource, 'projectRoot')

        String buildDotGradle = """
buildscript {
    dependencies {
        classpath files(new File(rootDir, '../../../libs/gogradle-${GogradleGlobal.GOGRADLE_VERSION}-all.jar'))
    }
}
System.setProperty('gradle.user.home','${StringUtils.toUnixString(userhome)}')

apply plugin: 'com.github.blindpirate.gogradle'

golang {
    buildMode = 'DEVELOP'
    packagePath = 'github.com/my/project'
    goVersion = '1.7.1'
    globalCacheFor 0,'second'
    goExecutable = '${StringUtils.toUnixString(goBinPath)}'
}

repositories {
    golang {
        root {it ==~ /github\\.com\\/\\w+\\/\\w+/}
        url {
            def array=it.split('/')
            return 'http://localhost:8080/'+array[1]+'-'+array[2]
        }
    }
}


dependencies {
    golang{
        build 'github.com/firstlevel/a'
        build(
            [name: 'github.com/firstlevel/b', version: '67b0cfae52118d8044c03c1564fd2845ba1b81e1'], // commit3
            'github.com/firstlevel/c@1.0.0'
        )

        build('github.com/firstlevel/d') {
            transitive = false
        }

    
        build(name: 'github.com/firstlevel/e', commit: '95907c7d') { // commit5
            transitive = true
            exclude name: 'github.com/external/e'
        }

        build name: 'github.com/firstlevel/f', dir: "${StringUtils.toUnixString(localDependencyRoot)}"

    }
}

"""

        writeBuildAndSettingsDotGradle(buildDotGradle)
    }


    @Test
    @WithGitRepos('git-repo.zip')
    void 'resolving dependencies of a complicated package should succeed'() {
        firstBuild()
        secondBuildWithUpToDate()
    }

    void firstBuild() {
        try {
            newBuild { build ->
                build.forTasks('installBuildDependencies', 'goDependencies')
            }
        } finally {
            println(stderr)
            println(stdout)
        }

        assertDependenciesAre([
                'github.com/firstlevel/a'    : 'commit2',
                'github.com/firstlevel/b'    : 'commit3',
                'github.com/firstlevel/c'    : 'commit3',
                'github.com/firstlevel/d'    : 'commit2',
                'github.com/firstlevel/e'    : 'commit5',

                // vendorexternal/a#1 and vendorexternal/a#2 exist in firstlevel/a#2's dependencies
                // and vendorexternal/a#1 wins because it is in vendor
                'github.com/vendorexternal/a': 'commit1',
                'github.com/vendorexternal/b': 'commit2',

                'github.com/vendoronly/a'    : 'commit2',

                // vendoronly/b#2 is newer
                'github.com/vendoronly/b'    : 'commit2',
                'github.com/vendoronly/c'    : 'commit2',
                'github.com/vendoronly/d'    : 'commit2',
                'github.com/vendoronly/e'    : 'commit2',
                'github.com/external/a'      : 'commit3',
                'github.com/external/b'      : 'commit4',
                'github.com/external/c'      : 'commit4',
                'github.com/external/d'      : 'commit4',
                'github.com/external/e'      : 'commit3',

        ])
    }

    void secondBuildWithUpToDate() {
        try {
            newBuild { build ->
                build.forTasks('installBuildDependencies')
            }
        } finally {
            println(stderr)
            println(stdout)
            assert stdout.toString().contains(':resolveBuildDependencies UP-TO-DATE')
            assert stdout.toString().contains(':installBuildDependencies UP-TO-DATE')
        }
    }

    @Test
    @WithGitRepos('git-repo.zip')
    void 'project-level cache should be used in second resolution'() {
        firstBuild()

        IOUtils.clearDirectory(new File(projectRoot, '.gogradle/build_gopath'))
        IOUtils.forceDelete(new File(projectRoot, '.gogradle/cache/build.bin'))
        IOUtils.forceDelete(new File(projectRoot, '.gogradle/cache/test.bin'))
        repositories.listFiles().each {
            if (!['firstlevel-a', 'firstlevel-c', 'firstlevel-d'].contains(it.name)) {
                IOUtils.forceDelete(it)
            }
        }
        IOUtils.clearDirectory(repositories)

        firstBuild()

        assert !stdout.toString().contains(':resolveBuildDependencies UP-TO-DATE')
        assert !stdout.toString().contains(':installBuildDependencies UP-TO-DATE')
    }

    @Override
    File getProjectRoot() {
        return projectRoot
    }

    void assertDependenciesAre(Map<String, String> finalDependencies) {
        finalDependencies.each { packageName, commit ->
            assert new File(projectRoot, ".gogradle/build_gopath/src/${packageName}/${commit}.go").exists()
        }
    }
}
