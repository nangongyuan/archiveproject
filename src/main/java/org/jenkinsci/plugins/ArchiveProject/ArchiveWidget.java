package org.jenkinsci.plugins.ArchiveProject;

import hudson.widgets.Widget;

import java.util.List;

public class ArchiveWidget extends Widget {

    private List<Backups> backupsList;

    public void setBackupsList(List<Backups> backupsList) {
        this.backupsList = backupsList;
    }

    public List<Backups> getBackupsList() {
        return backupsList;
    }

    public static class Backups {
        private String number;
        private String name;
        private String duration;

        public Backups(String number, String name, String duration) {
            this.number = number;
            this.name = name;
            this.duration = duration;
        }

        public Backups() {
            this.number = "0";
            this.name = "no name";
            this.duration = "zero";
        }

        public String getName() {
            return name;
        }

        public String getDuration() {
            return duration;
        }

        public String getNumber() {
            return number;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }
    }

}