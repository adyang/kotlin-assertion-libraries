package data

data class Dish(val name: String, val type: Type, val ingredients: Set<String>) {
    data class Type(val course: String, val taste: String)
}