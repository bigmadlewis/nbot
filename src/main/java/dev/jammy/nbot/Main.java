package dev.jammy.nbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("      DISCORD ACTIVITY BOT STARTING     ");
        System.out.println("========================================");

        Database.init();
        System.out.println("[1/4] Database Initialized...");

        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            System.err.println("Could not find config.properties! Create one with token=YOUR_TOKEN");
            return;
        }
        System.out.println("[2/4] Config Loaded...");

        String token = prop.getProperty("token");
        System.out.println("[3/4] Extracting Token");
        if (token.equals("YOUR_TOKEN_HERE")) {
            System.out.println("Invalid token, please replace YOUR_TOKEN_HERE with your own token in config.properties");
            return;
        }

        var jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new ActivityListener())
                .build();

        System.out.println("[4/4] Connecting to Discord...");

        jda.updateCommands().addCommands(
                Commands.slash("setup", "Import all current server members into the activity database"),
                Commands.slash("check", "See how many users have been inactive")
                        .addOption(OptionType.INTEGER, "days", "Amount of days to check for", true),
                Commands.slash("assign", "Assign a role to all inactive users")
                        .addOption(OptionType.INTEGER, "days", "Amount of days of inactivity", true)
                        .addOption(OptionType.ROLE, "role", "The role to assign", true),
                Commands.slash("assignif", "Assign a role to inactive users who already have a specific role")
                        .addOption(OptionType.INTEGER, "days", "Amount of days of inactivity", true)
                        .addOption(OptionType.ROLE, "required_role", "Only assign to users who have this role", true)
                        .addOption(OptionType.ROLE, "assign_role", "The role to assign to matching users", true)
        ).queue();

        System.out.println("Bot is online and commands are registered!");
    }
}