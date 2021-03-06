package com.github.blindpirate.gogradle.core.dependency

import com.github.blindpirate.gogradle.core.UnrecognizedGolangPackage
import com.github.blindpirate.gogradle.core.exceptions.UnrecognizedPackageException
import org.junit.Test

class UnrecognizedNotationDependencyTest {

    UnrecognizedGolangPackage pkg = UnrecognizedGolangPackage.of('unrecognized')

    UnrecognizedNotationDependency dependency = UnrecognizedNotationDependency.of(pkg)

    @Test(expected = UnsupportedOperationException)
    void 'isFirstLevel is not supported'() {
        dependency.isFirstLevel()
    }

    @Test(expected = UnsupportedOperationException)
    void 'isConcrete is not supported'() {
        dependency.getCacheScope()
    }

    @Test(expected = UnrecognizedPackageException)
    void 'getTransitiveDepExclusions is not supported'() {
        dependency.getTransitiveDepExclusions()
    }

    @Test(expected = UnrecognizedPackageException)
    void 'resolve is not supported'() {
        dependency.resolve(null)
    }

    @Test
    void 'getting package should succeed'() {
        assert dependency.package.is(pkg)
    }
}
