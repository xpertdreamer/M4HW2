package org.example;

import org.checkerframework.checker.units.qual.A;
import org.example.commands.AppBotCommand;
import org.example.commands.BotCommonCommands;
import org.example.functions.FilterOperations;
import org.example.functions.ImagesOperation;
import org.example.utils.ImageUtils;
import org.example.utils.RgbMaster;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.example.utils.PhotoMessageUtils;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Bot extends TelegramLongPollingBot {
    HashMap<String, Message> messages = new HashMap<>();

    @Override
    public String getBotUsername() {
        return "BOTNAME";
    }

    @Override
    public String getBotToken() {
        return "TOKEN";
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        try {
            SendMessage responseTextMessage = runCommonCommand(message);
            if (responseTextMessage != null) {
                execute(responseTextMessage);
                return;
            }
            responseTextMessage = runPhotoMessage(message);
            if (responseTextMessage != null) {
                execute(responseTextMessage);
                return;
            }
            Object responseMediaMessage = runPhotoFilter(message);
            if (responseMediaMessage != null) {
                if (responseMediaMessage instanceof SendMediaGroup) {
                    execute((SendMediaGroup) responseMediaMessage);
                } else if (responseMediaMessage instanceof SendMessage) {
                    execute((SendMessage) responseMediaMessage);
                }
                return;
            }
        } catch (InvocationTargetException | IllegalAccessException | TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private SendMessage runCommonCommand(Message message) throws InvocationTargetException, IllegalAccessException {
        String text = message.getText();
        BotCommonCommands commands = new BotCommonCommands();
        Method[] classMethods = commands.getClass().getDeclaredMethods();
        for (Method method : classMethods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {
                AppBotCommand command = method.getAnnotation(AppBotCommand.class);
                if (command.name().equals(text)) {
                    method.setAccessible(true);
                    String responseText = (String) method.invoke(commands);
                    if (responseText != null) {
                        SendMessage sendMessage = new SendMessage();
                        sendMessage.setChatId(message.getChatId().toString());
                        sendMessage.setText(responseText);
                        return sendMessage;
                    }
                }
            }
        }
        return null;
    }

    private SendMessage runPhotoMessage(Message message) {
        List<File> files = getFilesByMessage(message);
        if (files.isEmpty()) {
            return null;
        }
        String chatId = message.getChatId().toString();
        messages.put(chatId, message);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>(getKeyboardRows(FilterOperations.class));
        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите фильтр, который хотели бы применить к фото");
        return sendMessage;
    }

    private Object runPhotoFilter(Message newMessage) {
        final String text = newMessage.getText();
        ImagesOperation operation = ImageUtils.getOperation(text);
        if (operation == null) return null;
        String chatId = newMessage.getChatId().toString();
        Message photoMessage = messages.get(chatId);
        if (photoMessage != null) {
            List<File> files = getFilesByMessage(photoMessage);
            try {
                List<String> paths = PhotoMessageUtils.savePhotos(files, getBotToken());
                return preparePhotoMessage(paths, operation, chatId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("So just send a photo and ill process it");
            return sendMessage;
        }
        return null;
    }

    public void processingImage(String fileName, ImagesOperation operation) throws Exception {
        final BufferedImage image = ImageUtils.getImage(fileName);
        final RgbMaster rgbMaster = new RgbMaster(image);
        rgbMaster.changeImage(operation);
        ImageUtils.saveImage(rgbMaster.getImage(), fileName);
    }

    private SendMediaGroup preparePhotoMessage(List<String> localPaths, ImagesOperation operation, String chatId) throws Exception {
        SendMediaGroup mediaGroup = new SendMediaGroup();
        ArrayList<InputMedia> medias = new ArrayList<>();
        for (String path : localPaths) {
            InputMedia inputMedia = new InputMediaPhoto();
            processingImage(path, operation);
            inputMedia.setMedia(new java.io.File(path), "path");
            medias.add(inputMedia);
        }
        mediaGroup.setMedias(medias);
        mediaGroup.setChatId(chatId);
        return mediaGroup;
    }

    private List<File> getFilesByMessage(Message message) {
        List<PhotoSize> photoSizes = message.getPhoto();
        if (photoSizes == null) return new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        for (PhotoSize photoSize : photoSizes) {
            final String fileId = photoSize.getFileId();
            try {
                files.add(sendApiMethod(new GetFile(fileId)));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        return files;
    }

    private ReplyKeyboardMarkup getKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        ArrayList<KeyboardRow> allKeyboardRows = new ArrayList<>();
        allKeyboardRows.addAll(getKeyboardRows(BotCommonCommands.class));
        allKeyboardRows.addAll(getKeyboardRows(FilterOperations.class));

        replyKeyboardMarkup.setKeyboard(allKeyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        return replyKeyboardMarkup;
    }

    private static ArrayList<KeyboardRow> getKeyboardRows(Class someClass) {
        Method[] classMethods = someClass.getDeclaredMethods();
        ArrayList<AppBotCommand> commands = new ArrayList<>();
        for (Method method : classMethods) {
            if (method.isAnnotationPresent(AppBotCommand.class)) {
                commands.add(method.getAnnotation(AppBotCommand.class));
            }
        }
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        int columCount = 3;
        int rowCount = commands.size() / columCount + ((commands.size() % columCount == 0) ? 0 : 1);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            KeyboardRow row = new KeyboardRow();
            for (int columIndex = 0; columIndex < columCount; columIndex++) {
                int index = rowIndex * columCount + columIndex;
                if (index >= commands.size()) continue;
                AppBotCommand command = commands.get(rowIndex * columCount + columIndex);
                KeyboardButton keyboardButton = new KeyboardButton(command.name());
                row.add(keyboardButton);
            }
            keyboardRows.add(row);
        }
        return keyboardRows;
    }
}