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

/**
 * Utility methods for input checking.
 *
 * @author TOTHTOMI
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Asserts {

    public static void state(boolean expression, String errorMsg) throws IllegalStateException {
        if (!expression) {
            throw new IllegalStateException(errorMsg);
        }
    }

    /**
     * Checks whether the supplied object is null or not.
     * If it is null, an exception ({@link IllegalArgumentException}) will be thrown.
     *
     * @param obj      the object to check
     * @param errorMsg the message to throw in exception if object is null
     * @throws IllegalStateException if the check fails
     */
    public static void notNull(Object obj, String errorMsg) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * Checks whether the supplied object is null or not.
     * If it is null, an exception ({@link IllegalArgumentException}) will be thrown, otherwise if the array is empty,
     * the same exception will follow as well.
     *
     * @param array        the object to check
     * @param errorMessage the message to throw in exception if object is null
     * @throws IllegalStateException if the check fails
     */
    public static void notEmpty(Object[] array, String errorMessage) throws IllegalArgumentException {
        notNull(array, "Object to check must not be null!");

        if (array.length == 0) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}