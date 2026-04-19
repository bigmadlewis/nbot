package dev.jammy.nbot;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.sql.*;

public class ActivityListener extends ListenerAdapter {
    // nbot
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Database.updateActivity(event.getAuthor().getId());
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Database.updateActivity(event.getUser().getId());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) return;

        if (event.getName().equals("setup")) {
            event.deferReply().queue();

            event.getGuild().loadMembers().onSuccess(members -> {
                int count = 0;
                for (var member : members) {
                    if (!member.getUser().isBot()) {
                        Database.updateActivity(member.getId());
                        count++;
                    }
                }
                event.getHook().sendMessage("✅ Imported **" + count + "** members into the database. Their activity is set to today — inactivity tracking starts from now.").queue();
            });
            return;
        }

        int days = event.getOption("days").getAsInt();
        long threshold = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

        if (event.getName().equals("check")) {
            int count = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:activity.db");
                 PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM activity WHERE last_active < ?")) {
                pstmt.setLong(1, threshold);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) count = rs.getInt(1);
            } catch (SQLException e) { e.printStackTrace(); }

            event.reply("🔎 Found **" + count + "** users inactive for " + days + "+ days.").queue();
        }

        else if (event.getName().equals("assign")) {
            Role role = event.getOption("role").getAsRole();
            event.deferReply().queue();

            int totalAssigned = 0;
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:activity.db");
                 PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM activity WHERE last_active < ?")) {
                pstmt.setLong(1, threshold);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    event.getGuild().retrieveMemberById(userId).queue(member -> {
                        if (!member.getRoles().contains(role)) {
                            event.getGuild().addRoleToMember(member, role).queue();
                        }
                    }, throwable -> {});
                    totalAssigned++;
                }
            } catch (SQLException e) { e.printStackTrace(); }

            event.getHook().sendMessage("✅ Processed **" + totalAssigned + "** users. If eligible, they now have the " + role.getName() + " role.").queue();
        }

        else if (event.getName().equals("assignif")) {
            Role assignRole = event.getOption("assign_role").getAsRole();
            Role requiredRole = event.getOption("required_role").getAsRole();
            event.deferReply().queue();

            int[] totalAssigned = {0};
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:activity.db");
                 PreparedStatement pstmt = conn.prepareStatement("SELECT user_id FROM activity WHERE last_active < ?")) {
                pstmt.setLong(1, threshold);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String userId = rs.getString("user_id");
                    event.getGuild().retrieveMemberById(userId).queue(member -> {
                        if (member.getRoles().contains(requiredRole) && !member.getRoles().contains(assignRole)) {
                            event.getGuild().addRoleToMember(member, assignRole).queue();
                            totalAssigned[0]++;
                        }
                    }, throwable -> {});
                }
            } catch (SQLException e) { e.printStackTrace(); }

            event.getHook().sendMessage("✅ Assigned **" + assignRole.getName() + "** to inactive users who had the **"
                    + requiredRole.getName() + "** role (inactive for " + days + "+ days).").queue();
        }
    }
}