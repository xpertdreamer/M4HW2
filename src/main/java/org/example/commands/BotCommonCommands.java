package org.example.commands;

public class BotCommonCommands {
    @AppBotCommand(name = "/hello", description = "when request hello", showInHelp = true)
    String hello() {
        return "Hello, friend!";
    }

    @AppBotCommand(name = "/bye", description = "when request bye", showInHelp = true)
    String bye() {
        return "Are you leaving :( Comeback soon!";
    }

    @AppBotCommand(name = "/help", description = "when request help", showInKeyboard = true)
    String help() {
        return "You used command /help. Im working on it";
    }
}
