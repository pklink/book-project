/*
    The book project lets a user keep track of different books they would like to read, are currently
    reading, have read or did not finish.
    Copyright (C) 2020  Karan Kumar

    This program is free software: you can redistribute it and/or modify it under the terms of the
    GNU General Public License as published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY
    WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
    PURPOSE.  See the GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along with this program.
    If not, see <https://www.gnu.org/licenses/>.
 */

package com.karankumar.bookproject.backend.utils;

import static com.karankumar.bookproject.backend.utils.ShelfUtils.ALL_BOOKS_SHELF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;

import com.karankumar.bookproject.backend.service.BookService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.karankumar.bookproject.annotations.IntegrationTest;
import com.karankumar.bookproject.backend.entity.Author;
import com.karankumar.bookproject.backend.entity.Book;
import com.karankumar.bookproject.backend.entity.PredefinedShelf;
import com.karankumar.bookproject.backend.repository.BookRepository;
import com.karankumar.bookproject.backend.service.PredefinedShelfService;

import static com.karankumar.bookproject.backend.entity.PredefinedShelf.ShelfName.TO_READ;
import static com.karankumar.bookproject.backend.entity.PredefinedShelf.ShelfName.READING;
import static com.karankumar.bookproject.backend.entity.PredefinedShelf.ShelfName.READ;
import static com.karankumar.bookproject.backend.entity.PredefinedShelf.ShelfName.DID_NOT_FINISH;
import static com.karankumar.bookproject.backend.utils.PredefinedShelfUtils.isPredefinedShelf;

@IntegrationTest
class PredefinedShelfUtilsTest {
    private static BookRepository bookRepository;

    private static PredefinedShelfUtils predefinedShelfUtils;

    private static PredefinedShelf toReadShelf;
    private static PredefinedShelf readShelf;
    private static PredefinedShelf didNotFinishShelf;

    private static final Author NO_AUTHOR = null;

    private static Book book1;
    private static Book book2;
    private static Book book3;
    private static Book book4;

    private static List<String> PREDEFINED_SHELVES;
    private static final List<String> INVALID_SHELVES =
            List.of("Too read", "Readin", "Do not finish", "Shelf");
    private static final String ERROR_MESSAGE =
            "Shelf with name ''{0}'' does not match any predefined shelf";

    @BeforeAll
    public static void setupBeforeAll(@Autowired PredefinedShelfService predefinedShelfService,
                                      @Autowired BookRepository bookRepository,
                                      @Autowired BookService bookService) {
        predefinedShelfUtils = new PredefinedShelfUtils(predefinedShelfService);
        findPredefinedShelves();

        PredefinedShelfUtilsTest.bookRepository = bookRepository;
        resetBookRepository();
        createAndSaveBooks();

        PREDEFINED_SHELVES = predefinedShelfUtils.getPredefinedShelfNamesAsStrings();

        setBooksInPredefinedShelves();
    }

    private static void findPredefinedShelves() {
        toReadShelf = predefinedShelfUtils.findToReadShelf();
        readShelf = predefinedShelfUtils.findReadShelf();
        didNotFinishShelf = predefinedShelfUtils.findPredefinedShelf(DID_NOT_FINISH);
    }

    private static void resetBookRepository() {
        bookRepository.deleteAll();
    }

    private static void setBooksInPredefinedShelves() {
        toReadShelf.setBooks(Set.of(book1, book2));
        readShelf.setBooks(Set.of(book3));
        didNotFinishShelf.setBooks(Set.of(book4));
    }

    private static void createAndSaveBooks() {
        book1 = bookRepository.save(new Book("someTitle", NO_AUTHOR, toReadShelf));
        book2 = bookRepository.save(new Book("someTitle2", NO_AUTHOR, toReadShelf));
        book3 = bookRepository.save(new Book("someOtherTitle", NO_AUTHOR, readShelf));
        book4 = bookRepository.save(new Book("yetAnotherTitle", NO_AUTHOR, didNotFinishShelf));
    }

    @Test
    void shouldGetAllPredefinedShelfNamesFromDatabase() {
        // given
        List<String> expectedShelfNames = List.of(
                TO_READ.toString(),
                READING.toString(),
                READ.toString(),
                DID_NOT_FINISH.toString()
        );

        // when
        List<String> shelfNames = predefinedShelfUtils.getPredefinedShelfNamesAsStrings();

        // then
        assertEquals(expectedShelfNames, shelfNames);
    }

    @Test
    void shouldGetBooksInOneChosenShelf() {
        // given
        Set<Book> expectedBooks = Set.of(book1, book2);

        // when
        Set<Book> actualBooks = predefinedShelfUtils.getBooksInChosenPredefinedShelf("To read");

        // then
        assertEquals(expectedBooks, actualBooks);
    }

    @Test
    void shouldGetAllBooksWhenChosenShelfIsAllShelves() {
        // given
        Set<Book> expectedBooks = Set.of(book1, book2, book3, book4);

        // when
        Set<Book> actualBooks =
                predefinedShelfUtils.getBooksInChosenPredefinedShelf(ALL_BOOKS_SHELF);

        // then
        assertEquals(expectedBooks, actualBooks);
    }

    @Test
    void shouldGetAllBooksInChosenShelves() {
        // given
        List<PredefinedShelf> predefinedShelves = List.of(toReadShelf, readShelf);
        Set<Book> expectedBooks = Set.of(book1, book2, book3);

        // when
        Set<Book> actualBooks = predefinedShelfUtils.getBooksInPredefinedShelves(predefinedShelves);

        // then
        assertEquals(expectedBooks.size(), actualBooks.size());
        assertTrue(actualBooks.containsAll(expectedBooks));
    }

    @Test
    void testValidPredefinedShelfNames() {
        SoftAssertions softly = new SoftAssertions();

        PREDEFINED_SHELVES.forEach(shelfName -> softly.assertThat(isPredefinedShelf(shelfName))
                                                      .as(MessageFormat
                                                              .format(ERROR_MESSAGE, shelfName))
                                                      .isTrue());

        softly.assertAll();
    }

    @Test
    void isPredefinedShelfWorksForLowerCase() {
        SoftAssertions softly = new SoftAssertions();

        PREDEFINED_SHELVES.stream()
                          .map(String::toLowerCase)
                          .forEach(shelfName ->
                                  softly.assertThat(isPredefinedShelf(shelfName))
                                        .as(MessageFormat.format(ERROR_MESSAGE, shelfName))
                                        .isTrue());

        softly.assertAll();
    }

    @Test
    void isPredefinedShelfWorksForUpperCase() {
        SoftAssertions softly = new SoftAssertions();

        PREDEFINED_SHELVES.stream()
                          .map(String::toUpperCase)
                          .forEach(shelfName ->
                                  softly.assertThat(isPredefinedShelf(shelfName))
                                        .as(MessageFormat.format(ERROR_MESSAGE, shelfName))
                                        .isTrue());

        softly.assertAll();
    }

    @Test
    void testInvalidShelfNames() {
        SoftAssertions softly = new SoftAssertions();

        INVALID_SHELVES.forEach(shelfName -> softly.assertThat(isPredefinedShelf(shelfName))
                                                   .as(MessageFormat
                                                           .format(ERROR_MESSAGE, shelfName))
                                                   .isFalse());

        softly.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"To read", "Reading", "Read", "Did not finish"})
    void testGetPredefinedShelfNameReturnsCorrectShelf(String shelfName) {
        System.out.println("Shelf = " + shelfName);
        PredefinedShelf.ShelfName expectedShelf = null;
        switch (shelfName) {
            case "To read":
                expectedShelf = TO_READ;
                break;
            case "Reading":
                expectedShelf = READING;
                break;
            case "Read":
                expectedShelf = READ;
                break;
            case "Did not finish":
                expectedShelf = DID_NOT_FINISH;
        }
        PredefinedShelf.ShelfName actualShelf =
                predefinedShelfUtils.getPredefinedShelfName(shelfName);
        assertEquals(expectedShelf, actualShelf);
    }
}
