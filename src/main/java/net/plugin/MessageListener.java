package net.plugin;

import org.bukkit.Bukkit;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {
	@Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
		Member member = event.getMember();
		if(member != null && member.hasPermission(Permission.ADMINISTRATOR)) {
			String message = event.getMessage().getContentDisplay();
			if(message.startsWith("!c ")) {
				message = message.replace("!c ", "");
				CoreServerOnline.comamndsToSend.add(message);
			}
        }
    }
}
