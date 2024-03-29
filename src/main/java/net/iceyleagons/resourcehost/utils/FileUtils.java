/*
 * Copyright 2021 Tamás Tóth
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package net.iceyleagons.resourcehost.utils;

import lombok.SneakyThrows;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Utility methods regarding files.
 * This class is in conjunction with {@link AdvancedFile}.
 *
 * P.S from TOTHTOMI:
 * <p>
 *     I use this class in many of my private projects & develop it along the way.<br>
 *     Now, I've decided to publish it under a permissive license, so others can have it's useful contents.
 * </p>
 *
 * @author TOTHTOMI
 * @version 2.0.0
 * @since 1.0.0
 */
public final class FileUtils {

    /**
     * Will return the file name, but without the extension.
     *
     * @param file the file
     * @return the name of the file
     * @throws IllegalArgumentException if the supplied file is null
     */
    public static String getFileNameWithoutExtension(File file) throws IllegalStateException {
        Asserts.notNull(file, "File must not be null!");

        return file.getName().replaceFirst("[.][^.]+$", "");
    }

    /**
     * Appends {@link System#lineSeparator()} to the supplied string and returns it.
     *
     * @param toWrite the string
     * @return the processed string
     */
    public static String writeLine(String toWrite) {
        return toWrite + System.lineSeparator();
    }

    /**
     * Method will try to create a file if it does not exist.
     * If any exceptions occur during this, an {@link IllegalStateException} will be thrown.
     *
     * @param file         the file
     * @param ignoreErrors if true no exceptions will be thrown (aka. errors will be ignored)
     * @throws IllegalStateException    if file creation fails and ignoreErrors is set to false
     * @throws IllegalArgumentException if the file is null
     */
    public static void createFileIfNotExists(File file, boolean ignoreErrors) throws IllegalStateException, IllegalArgumentException {
        Asserts.notNull(file, "File must not be null!");

        if (!file.exists()) {
            try {
                if (!file.createNewFile() && !ignoreErrors)
                    throw new IllegalStateException("Could not create file " + file.getName());
            } catch (IOException e) {
                if (!ignoreErrors)
                    throw new IllegalStateException("Could not create file " + file.getName(), e);
            }
        }
    }

    /**
     * Deletes a folder by walking down the file tree and deleting the deepest file then proceeding until reaching the start point.
     * <b>Passed variable can only be a directory, regular file will be ignored.</b>
     *
     * @param file the folder to delete
     */
    @SneakyThrows
    public static void deleteFolder(File file) {
        if (!file.isDirectory()) return;

        Files.walk(file.toPath())
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Method will try to create a folder if it does not exist.
     * If any exceptions occur during this, an {@link IllegalStateException} will be thrown.
     *
     * @param file         the file
     * @param ignoreErrors if true no exceptions will be thrown (aka. errors will be ignored)
     * @throws IllegalStateException    if file creation fails and ignoreErrors is set to false
     * @throws IllegalArgumentException if the file is null
     */
    public static void createFolderIfNotExists(File file, boolean ignoreErrors) throws IllegalStateException, IllegalArgumentException {
        Asserts.notNull(file, "File must not be null!");

        if (!file.exists()) {
            if (!file.mkdirs() && !ignoreErrors)
                throw new IllegalStateException("Could not create file " + file.getName());
        }
    }

    /**
     * Appends the specified lines to the file using {@link FileWriter}.
     *
     * @param file the file to append to
     * @param linesToAppend the lines to append
     * @throws IllegalStateException if an exception occurs during appending
     * @throws IllegalArgumentException if the passed file is null or the lines are empty
     */
    public static void appendFile(File file, String... linesToAppend) throws IllegalStateException, IllegalArgumentException {
        Asserts.notNull(file, "File must not be null!");
        Asserts.notEmpty(linesToAppend, "LinesToAppend must not be empty!");

        FileUtils.createFileIfNotExists(file, false);

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.append(String.join(System.lineSeparator(), linesToAppend));
        } catch (IOException e) {
            throw new IllegalStateException("Error occurred while writing file content.", e);
        }
    }

    /**
     * Copies the content of the folder file into the destination folder file.
     * If a file listed in the folder is directory, it will call this method recursively.
     *
     * @param source      the source file
     * @param destination the destination file
     * @param ignore      the files to ignore
     * @throws IllegalStateException    if something happens during copying
     * @throws IllegalArgumentException if one of the files is null
     */
    public static void copyFolder(File source, File destination, String... ignore) throws IllegalStateException, IllegalArgumentException {
        Asserts.notNull(source, "Source file must not be null!");
        Asserts.notNull(destination, "Destination must not be null!");

        FileUtils.createFolderIfNotExists(source, false);
        FileUtils.createFolderIfNotExists(destination, false);
        List<String> blacklist = Arrays.asList(ignore);

        for (File f : Objects.requireNonNull(source.listFiles())) {
            if (f.canRead() && !blacklist.contains(f.getName())) {
                File copyFile = new File(destination, f.getName());

                if (f.isDirectory()) {
                    FileUtils.copyFolder(f, copyFile);
                    continue;
                }

                FileUtils.copyFileContent(f, copyFile);
            }
        }
    }

    /**
     * Copies the content of the source file into the destination file.
     *
     * @param source      the source file
     * @param destination the destination file
     * @throws IllegalStateException    if something happens during copying
     * @throws IllegalArgumentException if one of the files is null
     */
    public static void copyFileContent(File source, File destination) throws IllegalStateException, IllegalArgumentException {
        Asserts.notNull(source, "Source file must not be null!");
        Asserts.notNull(destination, "Destination must not be null!");

        FileUtils.createFileIfNotExists(source, false);
        FileUtils.createFileIfNotExists(destination, false);

        try (FileInputStream fileInputStream = new FileInputStream(source)) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(destination)) {
                byte[] buffer = new byte[1024];
                int length;

                while ((length = fileInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error occurred while copying file content.", e);
        }
    }
}