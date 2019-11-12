package com.github.giji34.worldgen;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin implements Listener {
    private GenerateTask task;

    @Override
    public void onLoad() {

    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable");
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {
        getLogger().info("onEnable");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player)sender;
        if (invalidGameMode(player)) {
            return false;
        }
        switch (label) {
            case "generate":
                return this.onGenerateCommand(player, args);
            case "generate-stop":
                return this.onGenerateStopCommand(player);
            default:
                return false;
        }
    }

    private boolean onGenerateCommand(Player player, String[] args) {
        if (this.task != null) {
            player.sendMessage(ChatColor.RED + "別の generate コマンドが実行中です");
            return true;
        }
        if (args.length != 4) {
            player.sendMessage(ChatColor.RED + "引数が足りません");
            return false;
        }
        int x0 = Integer.parseInt(args[0]) >> 4;
        int z0 = Integer.parseInt(args[1]) >> 4;
        int x1 = Integer.parseInt(args[2]) >> 4;
        int z1 = Integer.parseInt(args[3]) >> 4;
        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        int minZ = Math.min(z0, z1);
        int maxZ = Math.max(z0, z1);
        final World world = player.getWorld();
        Server server = getServer();
        this.task = new GenerateTask(world, minX, minZ, maxX, maxZ);
        server.getScheduler().runTaskTimer(this, task, 10, 1);
        return true;
    }

    boolean onGenerateStopCommand(Player player) {
        if (this.task == null) {
            player.sendMessage(ChatColor.RED + "実行中の generate コマンドはありません");
            return true;
        }
        this.task.cancel();
        return true;
    }

    class GenerateTask extends BukkitRunnable {
        final int minX;
        final int minZ;
        final int maxX;
        final int maxZ;
        final World world;
        boolean cancelSignaled;

        int x;
        int z;

        GenerateTask(World world, int minX, int minZ, int maxX, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.x = minX;
            this.z = minZ;
            this.cancelSignaled = false;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            long msPerTick = 1000 / 20;
            long maxElapsedMS = 5 * msPerTick;
            int count = 0;
            while (!cancelSignaled) {
                Chunk chunk = world.getChunkAt(x, z);
                if (!chunk.load()) {
                    chunk.load(true);
                    chunk.unload(true);
                }
                count++;
                long elapsed = System.currentTimeMillis() - start;
                z += 1;
                if (z > maxZ) {
                    z = minZ;
                    x += 1;
                    if (x > maxX) {
                        System.out.println("finished");
                        this.cancel();
                        break;
                    }
                }
                if (elapsed > maxElapsedMS) {
                    System.out.println("[" + x + ", " + z + "] " + count + " chunks; " + elapsed + " ms");
                    break;
                }
            }
        }

        @Override
        public void cancel() {
            System.out.println("cancel called");
            super.cancel();
            this.cancelSignaled = true;
        }
    }

    private boolean invalidGameMode(Player player) {
        GameMode current = player.getGameMode();
        return current != GameMode.CREATIVE && current != GameMode.SPECTATOR;
    }
}
