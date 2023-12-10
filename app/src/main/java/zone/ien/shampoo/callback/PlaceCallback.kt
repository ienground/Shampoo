package zone.ien.shampoo.callback

import zone.ien.shampoo.room.PlaceEntity

interface PlaceCallback {
    fun delete(position: Int, id: Long)
    fun edit(position: Int, entity: PlaceEntity)
}