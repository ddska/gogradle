package com.github.blindpirate.gogradle.core.dependency;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.github.blindpirate.gogradle.util.StringUtils.isPrefix;
import static com.github.blindpirate.gogradle.util.StringUtils.toUnixString;

public class DefaultDependencyRegistry implements DependencyRegistry {

    private Map<String, ResolvedDependency> packages = new HashMap<>();

    @Override
    public boolean register(ResolvedDependency dependencyToResolve) {
        synchronized (packages) {
            ResolvedDependency existent = retrieve(dependencyToResolve.getName());
            if (existent == null) {
                return registerSucceed(dependencyToResolve);
            } else if (isPrefix(existent.getName(), dependencyToResolve.getName())) {
                throw new IllegalStateException("Package " + existent.getName()
                        + " conflict with " + dependencyToResolve.getName());
            } else if (theyAreAllFirstLevel(existent, dependencyToResolve)) {
                if (theyAreAllVendor(existent, dependencyToResolve)) {
                    if (existentDependencyIsOutOfDate(existent, dependencyToResolve)) {
                        return registerSucceed(dependencyToResolve);
                    } else {
                        return false;
                    }
                } else {
                    throw new IllegalStateException("First-level package " + dependencyToResolve.getName()
                            + " conflict!");
                }
            } else if (existent.isFirstLevel()) {
                return false;
            } else if (dependencyToResolve.isFirstLevel()) {
                return registerSucceed(dependencyToResolve);
            } else if (existentDependencyIsOutOfDate(existent, dependencyToResolve)) {
                return registerSucceed(dependencyToResolve);
            } else {
                return false;
            }
        }
    }

    private boolean registerSucceed(ResolvedDependency resolvedDependency) {
        packages.put(resolvedDependency.getName(), resolvedDependency);
        return true;
    }

    @Override
    public ResolvedDependency retrieve(String name) {
        Path path = Paths.get(name);
        for (int i = path.getNameCount(); i > 0; i--) {
            Path subpath = path.subpath(0, i);
            ResolvedDependency ret = packages.get(toUnixString(subpath));
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    private boolean existentDependencyIsOutOfDate(ResolvedDependency existingDependency,
                                                  ResolvedDependency resolvedDependency) {
        return existingDependency.getUpdateTime() < resolvedDependency.getUpdateTime();
    }

    private boolean theyAreAllFirstLevel(ResolvedDependency existedModule, ResolvedDependency resolvedDependency) {
        return existedModule.isFirstLevel() && resolvedDependency.isFirstLevel();
    }

    private boolean theyAreAllVendor(ResolvedDependency existedDependency, ResolvedDependency dependencyToRegister) {
        return existedDependency instanceof VendorResolvedDependency
                && dependencyToRegister instanceof VendorResolvedDependency;
    }
}
