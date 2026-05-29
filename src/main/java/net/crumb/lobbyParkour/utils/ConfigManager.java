package net.crumb.lobbyParkour.utils;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private static FileConfiguration config;

    private static void requireLoaded() {
        if (config == null) {
            throw new IllegalStateException("ConfigManager used before config was loaded");
        }
    }

    public static void loadConfig(FileConfiguration config) {
        ConfigManager.config = config;
    }

    public static Format getFormat() {
        requireLoaded();
        return new Format();
    }

    public static Items getItems() {
        requireLoaded();
        return new Items();
    }

    public static Settings getSettings() {
        requireLoaded();
        return new Settings();
    }

    public static boolean isHologramsDisabled() {
        requireLoaded();
        return config.getBoolean("disable-holograms", false);
    }

    public static class Items {
        private final String checkpointPath = "items.last-checkpoint.";
        private final String resetPath = "items.reset.";
        private final String leavePath = "items.leave.";

        public String getCheckpointItem() {
            return config.getString(checkpointPath + "item", "minecraft:heavy_weighted_pressure_plate");
        }

        public int getCheckpointAmount() {
            return config.getInt(checkpointPath + "amount", 1);
        }

        public String getCheckpointName() {
            return config.getString(checkpointPath + "name", "<green>Last Checkpoint");
        }


        public String getResetItem() {
            return config.getString(resetPath + "item", "minecraft:oak_door");
        }

        public int getResetAmount() {
            return config.getInt(resetPath + "amount", 1);
        }

        public String getResetName() {
            return config.getString(resetPath + "name", "<red>Reset");
        }


        public String getLeaveItem() {
            return config.getString(leavePath + "item", "minecraft:red_bed");
        }

        public int getLeaveAmount() {
            return config.getInt(leavePath + "amount", 1);
        }

        public String getLeaveName() {
            return config.getString(leavePath + "name", "<red>Leave");
        }
    }

    public static class Format {
        private final String path = "formatting.";

        public String getStartPlate() {
            return config.getString("formatting.start-plate", "<green>\u2691</green> <white>%parkour_name%</white>");
        }

        public String getEndPlate() {
            return config.getString("formatting.end-plate", "<red>\u2691</red> <white>%parkour_name%</white>");
        }

        public String getCheckpointPlate() {
            return config.getString("formatting.checkpoint-plate");
        }

        public String getTimer() {
            return config.getString("formatting.timer", "%m%:%s%:%ms%");
        }

        public String getStartMessage() {
            return config.getString("formatting.start-message");
        }

        public String getCancelMessage() {
            return config.getString("formatting.cancel-message");
        }

        public String getEndMessage() {
            return config.getString("formatting.end-message");
        }

        public String getResetMessage() {
            return config.getString("formatting.reset-message");
        }

        public String getTpMessage() {
            return config.getString("formatting.tp-message");
        }

        public String getCheckpointMessage() {
            return config.getString("formatting.checkpoint-message");
        }

        public String getActionBar() {
            return config.getString("formatting.action-bar", "<color:#7ae0ff>%timer%</color> <color:#39aacc>\u231a</color>   <dark_gray>|</dark_gray>   <color:#54ff7f><color:#57ff65>%checkpoint%</color></color><color:#b8b8b8>/%checkpoint_total%</color> <green>\u2691</green>");
        }

        public String getCheckpointSkipMessage() {
            return config.getString(path + "checkpoint-skip-message");
        }

        public String getFlightCancelMessage() {
            return config.getString(path + "flight-cancel-message");
        }

        public String getFlightStartCancelMessage() {
            return config.getString(path + "flight-start-cancel-message");
        }

        public Leaderboard getLeaderboard() {
            return new Leaderboard("formatting.leaderboard.");
        }

        public static class Leaderboard {
            private final String path;

            public Leaderboard(String basePath) {
                this.path = basePath;
            }

            public String getTitle() {
                return config.getString(this.path + "title");
            }

            public int getMaximumDisplayed() {
                return config.getInt(this.path + "maximum-displayed", 10);
            }

            public boolean isPersonalBestEnabled() {
                return config.getBoolean(this.path + "personal-best-enabled", false);
            }

            public String getDefaultLineStyle() {
                return config.getString(this.path + "default-line-style");
            }

            public String getPersonalBestStyle() {
                return config.getString(this.path + "personal-best-style");
            }

            public String getEmptyLineStyle() {
                return config.getString(this.path + "empty-line-style");
            }

            public List<String> getLines() {
                return config.getStringList(this.path + "lines");
            }

            public DisplayItem getDisplayItem() {
                return new DisplayItem(this.path + "display-item.");
            }

            public static class DisplayItem {
                private final String path;

                public DisplayItem(String basePath) {
                    this.path = basePath;
                }

                public boolean isEnabled() {
                    return config.getBoolean(this.path + "enabled", true);
                }

                public Material getItem() {
                    String itemName = config.getString(this.path + "item", "minecraft:clock")
                            .replace("minecraft:", "")
                            .toUpperCase();

                    Material material = Material.matchMaterial(itemName);

                    // Fallback if the material doesn't exist
                    return (material != null) ? material : Material.CLOCK;
                }

                public boolean hasEnchantGlint() {
                    return config.getBoolean(this.path + "enchant-glint", true);
                }

                public boolean isSpinEnabled() {
                    return config.getBoolean(this.path + "spin-enabled", false);
                }
            }
        }
    }

    public static class Settings {
        private final String path = "setings.";

        public int getLeaderboardUpdateRate() {
            return config.getInt("setings.leaderboard-update", 1);
        }

        public int getLeaderboardQueryRate() {
            return config.getInt("setings.leaderboard-query-update", 1);
        }

        public boolean isAllowFly() {
            return config.getBoolean("settings.allow-fly", false);
        }

        public boolean isLastCheckpointRotation() {
            return config.getBoolean("settings.last-checkpoint-rotation", false);
        }
    }
}

