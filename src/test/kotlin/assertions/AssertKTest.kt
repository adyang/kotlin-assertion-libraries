package assertions

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.stream.Stream
import kotlin.streams.toList

class AssertKTest {
    @Nested
    inner class SingleElementAssertions {
        @Test
        fun `one assertion`() {
            val message = """expected:<"[bye]"> but was:<"[hi]">"""
            assertErrorContains(message) {
                assertThat("hi").isEqualTo("bye")
            }
        }

        @Test
        fun `multiple chain assertions - softly only`() {
            val message = """
                    The following assertions failed (2 failures)
                    	org.opentest4j.AssertionFailedError: expected to contain:<"aye"> but was:<"hi">
                    	org.opentest4j.AssertionFailedError: expected [length]:<[1]> but was:<[2]> ("hi")
                    """.trimIndent()
            assertErrorContains(message) {
                assertThat("hi").all {
                    isEqualTo("hi")
                    contains("aye")
                    hasLength(1)
                }
            }
        }
    }

    @Nested
    inner class IterableAssertions {
        private val iterable: Iterable<String> = listOf("one", "two", "three")

        @Test
        fun `contains given values in any order`() {
            assertThat(iterable).all {
                containsAll("one")
                containsAll("one", "two")
                containsAll("one", "three", "two")
            }

            assertErrorContains(
                """
                expected to contain all:<["absent"]> but was:<["one", "two", "three"]>
                 elements not found:<["absent"]>
                """.trimIndent()
            ) { assertThat(iterable).containsAll("absent") }
        }

        @Test
        fun `contains exactly given values, nothing else and in order`() {
            val list = iterable as List<String> // In AssertK, containsExactly is only defined on List

            assertThat(list).containsExactly("one", "two", "three")

            assertErrorContains(
                """
                expected to contain exactly:<["one", "three", "two"]> but was:<["one", "two", "three"]>
                 at index:1 expected:<"three">
                 at index:2 unexpected:<"three">
                """.trimIndent()
            ) { assertThat(list).containsExactly("one", "three", "two") }
        }

        @Test
        fun `contains exactly given values, nothing else but in any order`() {
            assertThat(iterable).all {
                containsExactlyInAnyOrder("three", "two", "one")
                containsExactlyInAnyOrder("two", "three", "one")
                containsExactlyInAnyOrder("one", "three", "two")
            }

            assertErrorContains(
                """
                expected to contain exactly in any order:<["one", "three"]> but was:<["one", "two", "three"]>
                 extra elements found:<["two"]>
                """.trimIndent()
            ) { assertThat(iterable).containsExactlyInAnyOrder("one", "three") }
        }

        @Test
        fun `contains only given values, nothing else but in any order and ignoring duplicates`() {
            val iterableWithDuplicates: Iterable<String> = listOf("one", "two", "three", "two")

            assertThat(iterableWithDuplicates).all {
                containsOnly("three", "two", "one")
                containsOnly("two", "three", "one")
                containsOnly("one", "three", "two", "two")
            }

            assertErrorContains(
                """
                expected to contain only:<["one", "three"]> but was:<["one", "two", "three", "two"]>
                 extra elements found:<["two", "two"]>
                """.trimIndent()
            ) { assertThat(iterableWithDuplicates).containsOnly("one", "three") }
        }

        @Test
        fun `contains any of (at least one of) the given values`() {
            assertThat(iterable).all {
                // AssertK does not have containsAnyOf, thus any and isIn are required to simulate similar behaviour
                any { it.isIn("three", "two") }
                any { it.isIn("one", "absentOne", "absentTwo") }
            }

            assertErrorContains(
                """
                expected any item to pass (3 failures)
                	org.opentest4j.AssertionFailedError: expected [[0]]:<["absentOne", "absentTwo", "absentThree"]> to contain:<"one"> (["one", "two", "three"])
                	org.opentest4j.AssertionFailedError: expected [[1]]:<["absentOne", "absentTwo", "absentThree"]> to contain:<"two"> (["one", "two", "three"])
                	org.opentest4j.AssertionFailedError: expected [[2]]:<["absentOne", "absentTwo", "absentThree"]> to contain:<"three"> (["one", "two", "three"])
                """.trimIndent()
            ) { assertThat(iterable).any { it.isIn("absentOne", "absentTwo", "absentThree") } }
        }

        @Test
        fun `contains sequence in correct order but with no extra values between sequence values`() {
            val list = iterable as List<String> // In AssertK, sequence assertion is only present in the form of subList

            assertThat(list).all {
                containsSubList(listOf("one", "two"))
                containsSubList(listOf("two", "three"))
            }

            assertErrorContains(
                """
                expected to contain the exact sublist (in the same order) as:<["one", "three"]>, but found none matching in:<["one", "two", "three"]>
                """.trimIndent()
            ) { assertThat(list).containsSubList(listOf("one", "three")) }
        }

        @Disabled("contains subsequence is not implemented in AssertK")
        @Test
        fun `contains subsequence in correct order and allows extra values between sequence values`() {
        }

        @Test
        fun `different iterables`() {
            assertThat(setOf("one", "two", "three")).all {
                containsOnly("three", "one", "two")
                containsExactlyInAnyOrder("three", "two", "one")
                containsAll("one", "three")
            }

            // As AssertK is a Kotlin library, there are no java.util.Stream specific assertions
            val list = Stream.of("one", "two", "three").toList()
            assertThat(list).all {
                containsOnly("three", "one", "two")
                containsExactlyInAnyOrder("three", "two", "one")
                containsAll("one", "three")
                containsExactly("one", "two", "three")
            }

            assertThat(arrayOf("one", "two", "three")).all {
                containsOnly("three", "one", "two")
                // containsExactlyInAnyOrder("three", "two", "one") is not implemented on Array in AssertK
                containsAll("one", "three")
                containsExactly("one", "two", "three")
            }
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
//                AssertK is not able to extract values via String names
//                assertThat(dishes)
//                    .extracting("name")
//                    .containsExactly("Keropok", "Prawn Mee", "Mango Pudding")

                assertThat(dishes)
                    .extracting(Dish::name).all {
                        containsExactly("Keropok", "Prawn Mee", "Mango Pudding")
                        doesNotContain("Brinjal")
                    }
            }

            @Test
            fun `multiple values per element`() {
                assertThat(dishes)
                    .extracting({ it.name }, { it.type.course })
                    .containsExactly(
                        Pair("Keropok", "Appetizer"),
                        Pair("Prawn Mee", "Main"),
                        Pair("Mango Pudding", "Dessert")
                    )

                assertThat(dishes)
                    .extracting(Dish::name, Dish::type)
                    .containsExactly(
                        Pair("Keropok", Dish.Type("Appetizer", "Savoury")),
                        Pair("Prawn Mee", Dish.Type("Main", "Savoury")),
                        Pair("Mango Pudding", Dish.Type("Dessert", "Sweet"))
                    )
            }

            @Test
            fun `flattening multiple values per element`() {
                assertThat(dishes)
                    .extracting(Dish::ingredients)
                    .transform { it.flatten() }
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
