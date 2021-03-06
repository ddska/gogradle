package com.github.blindpirate.gogradle.core.dependency;

import com.github.blindpirate.gogradle.GogradleGlobal;
import com.github.blindpirate.gogradle.core.GolangCloneable;
import com.github.blindpirate.gogradle.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Comparator.comparing;

public class GolangDependencySet implements Set<GolangDependency>, Serializable, GolangCloneable {

    private TreeSet<GolangDependency> container = new TreeSet<>(
            comparing((Serializable & Function<GolangDependency, String>) GolangDependency::getName));

    public GolangDependencySet(Collection<? extends GolangDependency> dependencies) {
        container.addAll(dependencies);
    }

    public GolangDependencySet() {
    }

    public static GolangDependencySet merge(GolangDependencySet... sets) {
        GolangDependencySet result = new GolangDependencySet();
        for (GolangDependencySet set : sets) {
            for (GolangDependency dependency : set) {
                if (!result.contains(dependency)) {
                    result.add(dependency);
                }
            }
        }
        return result;
    }

    public List<GolangDependency> flatten() {
        List<GolangDependency> result = new ArrayList<>();
        this.forEach((Serializable & Consumer<GolangDependency>)
                dependency -> dfs(dependency, result, 0));
        return result;
    }

    private void dfs(GolangDependency dependency, List<GolangDependency> result, int depth) {
        Assert.isTrue(depth < GogradleGlobal.MAX_DFS_DEPTH);
        result.add(dependency);

        if (dependency instanceof ResolvedDependency) {
            ResolvedDependency.class.cast(dependency)
                    .getDependencies()
                    .forEach((Serializable & Consumer<GolangDependency>)
                            subDependency -> dfs(subDependency, result, depth + 1));
        }
    }

    public static GolangDependencySet empty() {
        return new GolangDependencySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GolangDependencySet that = (GolangDependencySet) o;
        return Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(container);
    }

    @Override
    public int size() {
        return container.size();
    }

    @Override
    public boolean isEmpty() {
        return container.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return container.contains(o);
    }

    @Override
    public Iterator<GolangDependency> iterator() {
        return container.iterator();
    }

    @Override
    public Object[] toArray() {
        return container.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return container.toArray(a);
    }

    @Override
    public boolean add(GolangDependency dependency) {
        return container.add(dependency);
    }

    @Override
    public boolean remove(Object o) {
        return container.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return container.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends GolangDependency> c) {
        return container.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return container.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return container.removeAll(c);
    }

    @Override
    public void clear() {
        container.clear();
    }

    @Override
    public GolangDependencySet clone() {
        try {
            GolangDependencySet ret = (GolangDependencySet) super.clone();
            ret.container = new TreeSet<>(
                    comparing((Serializable & Function<GolangDependency, String>) GolangDependency::getName));

            this.container.forEach(item -> ret.add((GolangDependency) item.clone()));
            return ret;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
