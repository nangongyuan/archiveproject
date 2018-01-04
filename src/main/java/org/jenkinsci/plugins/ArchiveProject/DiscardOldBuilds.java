package org.jenkinsci.plugins.ArchiveProject;

import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.model.BuildDiscarder;
import jenkins.model.BuildDiscarderDescriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class DiscardOldBuilds extends BuildDiscarder {

    private final int numToKeep;

    public DiscardOldBuilds(int numToKeep) {
        this.numToKeep = numToKeep;
    }

    @Override
    public void perform(Job<?, ?> job) throws IOException {
        Run lsb = job.getLastSuccessfulBuild();
        Run lstb = job.getLastFailedBuild();

        List<? extends Run<?,?>> builds = job.getBuilds();
        for (Run r : copy(builds.subList(Math.min(builds.size(), numToKeep), builds.size()))){
            if (shouldKeepRun(r, lsb, lstb)) {
                continue;
            }
            r.delete();
        }
    }

    private <R> Collection<R> copy(Iterable<R> src) {
        return Lists.newArrayList(src);
    }

    private boolean shouldKeepRun(Run r, Run lsb, Run lstb) {
        if (r.isKeepLog()) {
            return true;
        }
        if (r == lsb) {
            return true;
        }
        if (r == lstb) {
            return true;
        }
        if (r.isBuilding()) {
            return true;
        }
        return false;
    }

    /*
    @Extension
    public static final class DOBDescriptor extends BuildDiscarderDescriptor {
    }
    */
}
