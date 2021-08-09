package com.company;

import com.sun.tools.javac.Main;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class Serializer<T> {

    //RU:тип хранения    //EN:storage type
    private final TypePath typePath;

    //RU:значение для Preferences \ путь к файлу    //EN:value for Preferences \ file path
    private final String path;
    private Preferences userPrefs;

    private final T defaultObject;//RU:дефолтный объект если файла \ значение для Preferences не существует
                            //EN:default object if file \ value for Preferences does not exist

    //RU:конструктор с дефолтным значением    //EN:constructor with default object
    public Serializer(String path, T defaultObject, TypePath typePath) {
        this.defaultObject = defaultObject;
        this.path = path;
        this.typePath = typePath;
        switch (typePath) {//RU:нужен ли Preferences
                           //EN:does the program need Preferences?
            case FILE, IN_JAR -> userPrefs = null;
            case PREFERENCES ->  userPrefs = Preferences.userNodeForPackage(getClass());
        }
    }

    //RU:конструктор без дефолтного объекта    //EN:default objectless constructor
    public Serializer(String path, TypePath typePath) {
        this.defaultObject = null;
        this.path = path;
        this.typePath = typePath;
        switch (typePath) {//RU:нужен ли Preferences
                           //EN:does the program need Preferences?
            case FILE, IN_JAR -> userPrefs = null;
            case PREFERENCES ->  userPrefs = Preferences.userNodeForPackage(Main.class);
        }
    }

    //RU:сохраняет объект    //EN:saves the object
    public void serializeData(T o) {
        try {
            if (typePath == TypePath.PREFERENCES) {//RU:если сохраняет внутрь программы
                                                   //EN:if it saves inside the program
                userPrefs.putByteArray(path, objectToByteArray(o));
            } else if (typePath == TypePath.FILE){//RU:если сохраняет в видимом для всех файле
                                                  //EN:if it saves in a file visible to all
                serializeWithStream(o);
            } else {
                throw new SaveInJarException();//RU:если пытается сохранить в jar файл
                                               //EN:if trying to save to jar file
            }
        } catch (IOException | SaveInJarException e) {
            e.printStackTrace();
        }

    }

    //RU:сохранение через видимый для всех файл    //EN:saving via a file visible to all
    private void serializeWithStream(T o) {
        try {
            StringBuilder folderPath = new StringBuilder(path.split("/")[0]);//RU:путь папок (без файла) на случай если их нет
                                                                                   //EN:path of folders (without file) in case they are not there
            for (int i = 1; i < path.split("/").length-1; i++) {
                folderPath.append("/").append(path.split("/")[i]);
            }
            Files.createDirectories(Paths.get(folderPath.toString()));//RU:создание папок, если они отсутствуют
                                                                      //EN:creating folders if they are missing
            FileOutputStream out = new FileOutputStream(path);//RU:запись объекта в файл
                                                              //EN:writing an object to a file
            ObjectOutputStream outObject = new ObjectOutputStream(out);
            outObject.writeObject(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //RU:получает объект    //EN:taking object
    public T deserializeData() {
        try {
            if (typePath == TypePath.PREFERENCES) {//RU:если получает объект изнутри программы
                                                   //EN:if it gets an object from inside the program
                return byteArrayToObject(userPrefs.getByteArray(path, objectToByteArray(defaultObject)));
            } else if (typePath == TypePath.FILE){//RU:если получает объект из видимого для всех файла
                                                  //EN:if it gets an object from a file visible to all
                return deserializeWithStream();
            } else {//RU:если получает изображение из jar файла
                    //EN:if it gets an image from a jar file
                return (T) Toolkit.getDefaultToolkit().getImage(getClass().getResource(path));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            //RU:возвращает дефолтное значение при исключении    //EN:returns default value on exception
            return defaultObject;
        }
    }

    //RU:получение объекта из видимого для всех файла    //EN:getting an object from a file visible to all
    private T deserializeWithStream() {
        try {//RU:если файл существует
             //EN:if file exists
            FileInputStream inFile = new FileInputStream(path);
            return byteArrayToObject(inFile.readAllBytes());
        } catch (IOException | ClassNotFoundException e) {//RU:если файла не существует
                                                          //EN:if the file doesn't exist
            e.printStackTrace();
            //RU:возвращает дефолтное значение при исключении    //EN:returns default value on exception
            return defaultObject;
        }
    }

    //RU:преобразует массив байтов в объект    //EN:converts a byte array to an object
    private T byteArrayToObject(byte[] array) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
        return (T) inputStream.readObject();
    }

    //RU:преобразует объект в массив байтов    //EN:converts an object to a byte array
    private byte[] objectToByteArray(T object) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteStream);
        outputStream.writeObject(object);
        return byteStream.toByteArray();
    }

    //RU:тип хранения    //EN:storage type
    public enum TypePath {
        PREFERENCES,//RU:сохраняет внутри программы (подойдёт для сохранения настроек и игровых значений (например монет))
                    //EN:saves inside the program (suitable for saving settings and game values (for example, coins))
        FILE,//RU:сохраняет в видимом для всех файле (подойдёт для всего)
             //EN:saves in a file visible to all (suitable for everything)
        IN_JAR//RU:сохраняет в jar файле, файлы в нём нельзя изменить (потому что я не знаю как это делать), а только достать (подойдёт для изображений, моделей и т.д.)
              //EN:saves in a jar file, the files in it cannot be changed (because I do not know how to do this), but only to get it (suitable for images, models, etc.)
    }
    //RU:исключение при попытке сериализации в jar файл    //EN:an exception when trying to serialize to a jar file
    public static class SaveInJarException extends Exception {
        public SaveInJarException() {
            super("EN:the author does not know how to save objects in a jar file" + "\n" +
                    "                                           RU:автор не умеет сохранять объекты в jar файле");
        }
    }
}
