package assertions

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream

class AssertJTest {
    @Nested
    inner class SingleElementAssertions {
        @Test
        fun `one assertion`() {
            val message = """
                    expected: "bye"
                    but was : "hi"
                    """.trimIndent()
            assertErrorContains(message) { assertThat("hi").isEqualTo("bye") }
        }

        @Test
        fun `multiple chain assertions`() {
            val message = """
                    Expecting:
                      "hi"
                    to contain:
                      "aye"
                    """.trimIndent()
            assertErrorContains(message) { assertThat("hi").isEqualTo("hi").contains("aye").hasSize(1) }
        }
    }

    @Nested
    inner class IterableAssertions {
        private val iterable: Iterable<String> = listOf("one", "two", "three")

        @Test
        fun `contains given values in any order`() {
            assertThat(iterable)
                .contains("one")
                .contains("one", "two")
                .contains("one", "three", "two")

            assertErrorContains(
                """
                Expecting ArrayList:
                  ["one", "two", "three"]
                to contain:
                  ["absent"]
                but could not find the following element(s):
                  ["absent"]
                """.trimIndent()
            ) { assertThat(iterable).contains("absent") }
        }

        @Test
        fun `contains exactly given values, nothing else and in order`() {
            assertThat(iterable).containsExactly("one", "two", "three")

            assertErrorContains(
                """
                Actual and expected have the same elements but not in the same order, at index 1 actual element was:
                  "two"
                whereas expected element was:
                  "three"
                """.trimIndent()
            ) { assertThat(iterable).containsExactly("one", "three", "two") }
        }

        @Test
        fun `contains exactly given values, nothing else but in any order`() {
            assertThat(iterable)
                .containsExactlyInAnyOrder("three", "two", "one")
                .containsExactlyInAnyOrder("two", "three", "one")
                .containsExactlyInAnyOrder("one", "three", "two")

            assertErrorContains(
                """
                Expecting:
                  ["one", "two", "three"]
                to contain exactly in any order:
                  ["one", "three"]
                but the following elements were unexpected:
                  ["two"]
                """.trimIndent()
            ) { assertThat(iterable).containsExactlyInAnyOrder("one", "three") }
        }

        @Test
        fun `contains only given values, nothing else but in any order and ignoring duplicates`() {
            val iterableWithDuplicates: Iterable<String> = listOf("one", "two", "three", "two")

            assertThat(iterableWithDuplicates)
                .containsOnly("three", "two", "one")
                .containsOnly("two", "three", "one")
                .containsOnly("one", "three", "two", "two")

            assertErrorContains(
                """
                Expecting ArrayList:
                  ["one", "two", "three", "two"]
                to contain only:
                  ["one", "three"]
                but the following element(s) were unexpected:
                  ["two", "two"]
                """.trimIndent()
            ) { assertThat(iterableWithDuplicates).containsOnly("one", "three") }
        }

        @Test
        fun `contains any of (at least one of) the given values`() {
            assertThat(iterable)
                .containsAnyOf("three", "two")
                .containsAnyOf("one", "absentOne", "absentTwo")

            assertErrorContains(
                """
                Expecting:
                  ["one", "two", "three"]
                to contain at least one of the following elements:
                  ["absentOne", "absentTwo", "absentThree"]
                but none were found
                """.trimIndent()
            ) { assertThat(iterable).containsAnyOf("absentOne", "absentTwo", "absentThree") }
        }

        @Test
        fun `contains sequence in correct order but with no extra values between sequence values`() {
            assertThat(iterable)
                .containsSequence("one", "two")
                .containsSequence("two", "three")

            assertErrorContains(
                """
                Expecting:
                  ["one", "two", "three"]
                to contain sequence:
                  ["one", "three"]
                """.trimIndent()
            ) { assertThat(iterable).containsSequence("one", "three") }
        }

        @Test
        fun `contains subsequence in correct order and allows extra values between sequence values`() {
            assertThat(iterable)
                .containsSubsequence("one", "two")
                .containsSubsequence("one", "three")

            assertErrorContains(
                """
                Expecting:
                  ["one", "two", "three"]
                to contain subsequence:
                  ["one", "three", "three"]
                """.trimIndent()
            ) { assertThat(iterable).containsSubsequence("one", "three", "three") }
        }

        @Test
        fun `different iterables`() {
            assertThat(setOf("one", "two", "three"))
                .containsOnly("three", "one", "two")
                .containsExactlyInAnyOrder("three", "two", "one")
                .contains("one", "three")

            assertThat(Stream.of("one", "two", "three"))
                .containsOnly("three", "one", "two")
                .containsExactlyInAnyOrder("three", "two", "one")
                .contains("one", "three")
                .containsExactly("one", "two", "three")

            assertThat(arrayOf("one", "two", "three"))
                .containsOnly("three", "one", "two")
                .containsExactlyInAnyOrder("three", "two", "one")
                .contains("one", "three")
                .containsExactly("one", "two", "three")
        }

        @Nested
        inner class ExtractingValues {
            private val dishes = listOf(
                Dish("Keropok", Dish.Type("Appetizer", "Savoury"), setOf("Flour", "Fish")),
                Dish("Prawn Mee", Dish.Type("Main", "Savoury"), setOf("Prawn", "Noodle")),
                Dish("Mango Pudding", Dish.Type("Dessert", "Sweet"), setOf("Mango", "Gelatin")),
            )

            @Test
            fun `single value per element`() {
                assertThat(dishes)
                    .extracting("name")
                    .containsExactly("Keropok", "Prawn Mee", "Mango Pudding")

                assertThat(dishes)
                    .extracting<String>(Dish::name)
                    .containsExactly("Keropok", "Prawn Mee", "Mango Pudding")
                // .doesNotContain("Brinjal") Unable to chain assertion after extraction in Kotlin
            }

            @Test
            fun `multiple values per element`() {
                assertThat(dishes)
                    .extracting("name", "type.course")
                    .containsExactly(
                        tuple("Keropok", "Appetizer"),
                        tuple("Prawn Mee", "Main"),
                        tuple("Mango Pudding", "Dessert")
                    )

                assertThat(dishes)
                    .extracting(Dish::name, Dish::type)
                    .containsExactly(
                        tuple("Keropok", Dish.Type("Appetizer", "Savoury")),
                        tuple("Prawn Mee", Dish.Type("Main", "Savoury")),
                        tuple("Mango Pudding", Dish.Type("Dessert", "Sweet"))
                    )
            }

            @Test
            fun `flattening multiple values per element`() {
                assertThat(dishes)
                    .flatExtracting("ingredients")
                    .containsExactly(
                        "Flour", "Fish",
                        "Prawn", "Noodle",
                        "Mango", "Gelatin"
                    )

                assertThat(dishes)
                    .flatExtracting<String>(Dish::ingredients)
                    .containsExactly(
                        "Flour", "Fish",
                        "Prawn", "Noodle",
                        "Mango", "Gelatin"
                    )
            }
        }
    }

    private fun assertErrorContains(message: String, assertion: () -> Unit) {
        val exception = assertThrows<AssertionError>(assertion)
        assertEquals(message, exception.message?.trim())
    }

    data class Dish(val name: String, val type: Type, val ingredients: Set<String>) {
        data class Type(val course: String, val taste: String)
    }
}
