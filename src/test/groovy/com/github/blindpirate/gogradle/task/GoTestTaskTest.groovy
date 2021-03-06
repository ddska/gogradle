package com.github.blindpirate.gogradle.task

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.support.WithResource
import com.github.blindpirate.gogradle.task.go.GoTestStdoutExtractor
import com.github.blindpirate.gogradle.task.go.GoTestTask
import com.github.blindpirate.gogradle.task.go.PackageTestContext
import com.github.blindpirate.gogradle.util.IOUtils
import com.github.blindpirate.gogradle.util.ReflectionUtils
import com.github.blindpirate.gogradle.util.StringUtils
import org.gradle.api.internal.tasks.testing.junit.result.TestClassResult
import org.gradle.api.internal.tasks.testing.junit.result.TestMethodResult
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestResult
import org.gradle.internal.operations.BuildOperationProcessor
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.util.function.Consumer

import static com.github.blindpirate.gogradle.task.GolangTaskContainer.INSTALL_BUILD_DEPENDENCIES_TASK_NAME
import static com.github.blindpirate.gogradle.task.GolangTaskContainer.INSTALL_TEST_DEPENDENCIES_TASK_NAME
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

@RunWith(GogradleRunner)
@WithResource('')
class GoTestTaskTest extends TaskTest {
    GoTestTask task

    File resource

    @Captor
    ArgumentCaptor<List> argumentsCaptor

    @Mock
    GoTestStdoutExtractor extractor

    @Mock
    BuildOperationProcessor buildOperationProcessor

    @Before
    void setUp() {
        task = buildTask(GoTestTask)
        when(project.getRootDir()).thenReturn(resource)
        when(setting.getPackagePath()).thenReturn('github.com/my/package')

        ReflectionUtils.setField(task, 'extractor', extractor)
        ReflectionUtils.setField(task, 'buildOperationProcessor', buildOperationProcessor)

        IOUtils.write(resource, 'a/a1_test.go', '')
        IOUtils.write(resource, 'a/_a1_test.go', '')
        IOUtils.write(resource, 'a/a2_test.go', '')
        IOUtils.write(resource, 'a/.a2_test.go', '')
        IOUtils.write(resource, 'a/a1.go', '')
        IOUtils.write(resource, 'a/a2.go', '')

        IOUtils.write(resource, 'b/b1_test.go', '')
        IOUtils.write(resource, 'b/b2_test.go', '')
        IOUtils.write(resource, 'b/b1.go', '')
        IOUtils.write(resource, 'b/b2.go', '')

        IOUtils.mkdir(resource, 'c/testdata/c_test.go')
        IOUtils.write(resource, 'c/c.go', '')
        IOUtils.write(resource, 'c/c.nongo', '')

        IOUtils.mkdir(resource, 'd')
        IOUtils.mkdir(resource, 'vendor')
    }

    @Test
    void 'test task should depend on install task'() {
        assertTaskDependsOn(task, INSTALL_TEST_DEPENDENCIES_TASK_NAME)
        assertTaskDependsOn(task, INSTALL_BUILD_DEPENDENCIES_TASK_NAME)
    }

    @Test
    void 'all package should be tested if not specified'() {
        // when
        task.addDefaultActionIfNoCustomActions()
        task.actions[0].execute(task)
        // then
        verify(buildManager, times(2)).go(argumentsCaptor.capture(), isNull(), any(Consumer), any(Consumer), any(Consumer))
        assert argumentsCaptor.getAllValues().contains(
                ['test', '-v', 'github.com/my/package/a',
                 "-coverprofile=${StringUtils.toUnixString(resource)}/.gogradle/reports/coverage/profiles/github.com%2Fmy%2Fpackage%2Fa".toString()])
        assert argumentsCaptor.getAllValues().contains(
                ['test', '-v', 'github.com/my/package/b',
                 "-coverprofile=${StringUtils.toUnixString(resource)}/.gogradle/reports/coverage/profiles/github.com%2Fmy%2Fpackage%2Fb".toString()])
    }

    @Test
    void 'coverage profiles should not be generated if not specified'() {
        // when
        task.addDefaultActionIfNoCustomActions()
        task.setGenerateCoverageProfile(false)
        task.actions[0].execute(task)
        // then
        assert !task.generateCoverageProfile
        verify(buildManager, times(2)).go(argumentsCaptor.capture(), isNull(), any(Consumer), any(Consumer), any(Consumer))
        assert argumentsCaptor.getAllValues().contains(['test', '-v', 'github.com/my/package/a'])
        assert argumentsCaptor.getAllValues().contains(['test', '-v', 'github.com/my/package/b'])
    }

    @Test
    void 'specific package should be tested if pattern is set'() {
        // when
        ReflectionUtils.setField(task, 'testNamePattern', ['a1*'])
        task.addDefaultActionIfNoCustomActions()
        task.actions[0].execute(task)
        // then
        verify(buildManager).go(argumentsCaptor.capture(), isNull(Map), any(Consumer), any(Consumer), any(Consumer))
        List<String> args = argumentsCaptor.getValue()
        assert args.size() == 6
        assert args[0..1] == ['test', '-v']
        assert ['a/a1_test.go', 'a/a1.go', 'a/a2.go'].any { args.contains(new File(resource, it).absolutePath) }
    }

    @Test
    void 'collecting stdout should succeed'() {
        // given
        ReflectionUtils.setField(task, 'testNamePattern', ['a1*'])
        when(buildManager.go(anyList(), isNull(Map), any(Consumer), any(Consumer), any(Consumer))).thenAnswer(new Answer<Object>() {
            @Override
            Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.arguments[2].accept('stdout')
                invocation.arguments[3].accept('stderr')
            }
        })
        ArgumentCaptor<PackageTestContext> argumentCaptor = ArgumentCaptor.forClass(PackageTestContext)
        // when
        task.addDefaultActionIfNoCustomActions()
        task.actions[0].execute(task)
        // then
        verify(extractor).extractTestResult(argumentCaptor.capture())
        PackageTestContext context = argumentCaptor.getValue()
        assert context.packagePath == 'github.com/my/package/a'
        assert context.stdout == ['stdout', 'stderr']
        assert context.testFiles.size() == 3
    }

    @Test
    void 'successful and failed result should be counted correctly'() {
        // given
        TestClassResult result = new TestClassResult(1L, 'className', 2L)
        result.add(new TestMethodResult(1L, 'methodName', TestResult.ResultType.FAILURE, 2L, 3L))
        result.add(new TestMethodResult(1L, 'methodName', TestResult.ResultType.SUCCESS, 2L, 3L))
        when(extractor.extractTestResult(any(PackageTestContext))).thenReturn([result])

        ReflectionUtils.setField(task, 'testNamePattern', ['a1*'])

        // when
        task.addDefaultActionIfNoCustomActions()
        try {
            task.actions[0].execute(task)
            assert false
        } catch (Exception e) {
            assert e.getMessage().contains('There are 1 failed tests')
        }
    }

    @Test
    void 'nothing should happened if no matched tests found'() {
        // when
        task.setTestNamePattern(['unexistent'])
        task.addDefaultActionIfNoCustomActions()
        // then
        task.actions.size() == 0
    }

    @Test(expected = UnsupportedOperationException)
    void 'leftShift should not be supported!'() {
        task.leftShift {}
    }

    @Test
    void 'doLast and doFirst should be warned'() {
        // given
        Logger mockLogger = mock(Logger)
        ReflectionUtils.setStaticFinalField(GoTestTask, 'LOGGER', mockLogger)
        // when
        task.doFirst {}
        task.doLast {}
        // then
        verify(mockLogger, times(2)).warn('WARNING: test report is not supported in customized test action.')
    }

    @Test
    void 'rewriting html reports should succeed'() {
        // given
        String html = '<body></body>'
        List files = ['index.html', 'sub/index.html', 'sub/sub/index.html', 'sub/sub/index.nohtml']
        files.each { IOUtils.write(resource, it, html) }
        // when
        task.addDefaultActionIfNoCustomActions()
        task.rewritePackageName(resource)
        // then
        files[0..2].each { assert new File(resource, it).text.contains('<script>') }
        assert new File(resource, 'sub/sub/index.nohtml').text == html
    }
}
