package com.github.blindpirate.gogradle.core.dependency.produce

import com.github.blindpirate.gogradle.GogradleRunner
import com.github.blindpirate.gogradle.core.dependency.GolangDependencySet
import com.github.blindpirate.gogradle.core.dependency.ResolvedDependency
import com.github.blindpirate.gogradle.core.dependency.produce.strategy.DependencyProduceStrategy
import com.github.blindpirate.gogradle.support.WithResource
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

import static org.mockito.Mockito.when

@RunWith(GogradleRunner)
class DefaultDependencyVisitorTest {
    @Mock
    ExternalDependencyFactory external1
    @Mock
    ExternalDependencyFactory external2
    @Mock
    SourceCodeDependencyFactory sourceCodeDependencyFactory
    @Mock
    VendorDependencyFactory vendorDependencyFactory
    @Mock
    ResolvedDependency resolvedDependency
    @Mock
    GolangDependencySet dependencySet
    @Mock
    DependencyProduceStrategy strategy
    @Mock
    File rootDir
    File resource

    DefaultDependencyVisitor visitor

    @Before
    void setUp() {
        visitor = new DefaultDependencyVisitor(
                [external1, external2],
                sourceCodeDependencyFactory,
                vendorDependencyFactory
        )
    }

    @Test
    void 'visiting external dependencies should succeed'() {
        // given:
        when(external1.produce(rootDir, 'build')).thenReturn(Optional.empty())
        when(external2.produce(rootDir, 'build')).thenReturn(Optional.of(dependencySet))

        // then:
        assert visitor.visitExternalDependencies(resolvedDependency, rootDir, 'build') == dependencySet
    }

    @Test
    void 'visiting source dependencies should succeed'() {
        // given:
        when(sourceCodeDependencyFactory.produce(resolvedDependency, rootDir, 'build')).thenReturn(dependencySet)
        // then:
        assert visitor.visitSourceCodeDependencies(resolvedDependency, rootDir, 'build') == dependencySet
    }

    @Test
    void 'visiting vendor dependencies should succeed'() {
        // given:
        when(vendorDependencyFactory.produce(resolvedDependency, rootDir)).thenReturn(GolangDependencySet.empty())
        // then:
        assert visitor.visitVendorDependencies(resolvedDependency, rootDir, 'build').isEmpty()

        //given:
        when(vendorDependencyFactory.produce(resolvedDependency, rootDir)).thenReturn(dependencySet)
        // then:
        assert visitor.visitVendorDependencies(resolvedDependency, rootDir, 'build') == dependencySet
    }

    @Test
    @WithResource('')
    void 'empty set should be returned when no external dependencies exist'() {
        assert visitor.visitExternalDependencies(resolvedDependency, resource, 'build').isEmpty()
    }

}
