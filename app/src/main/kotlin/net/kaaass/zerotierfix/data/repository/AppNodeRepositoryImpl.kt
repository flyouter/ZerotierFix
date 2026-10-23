package net.kaaass.zerotierfix.data.repository

import net.kaaass.zerotierfix.data.database.AppDatabase
import net.kaaass.zerotierfix.data.database.DatabaseUtils
import net.kaaass.zerotierfix.data.database.entity.AppNode

import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AppNodeRepository] 的 Room 实现。
 */
@Singleton
class AppNodeRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : AppNodeRepository {

    override fun getNodeIdStr(): String? {
        DatabaseUtils.readLock.lock()
        try {
            val nodes = database.appNodeDao().loadAll()
            return if (nodes.isEmpty()) null else nodes[0].nodeIdStr
        } finally {
            DatabaseUtils.readLock.unlock()
        }
    }

    override fun saveNodeId(nodeId: Long) {
        DatabaseUtils.writeLock.lock()
        try {
            database.runInTransaction {
                val nodes = database.appNodeDao().loadAll()
                if (nodes.isEmpty()) {
                    database.appNodeDao().insert(
                        AppNode(nodeId, String.format("%10x", nodeId))
                    )
                } else {
                    val node = nodes[0]
                    node.nodeId = nodeId
                    node.nodeIdStr = String.format("%10x", nodeId)
                    database.appNodeDao().update(node)
                }
            }
        } finally {
            DatabaseUtils.writeLock.unlock()
        }
    }
}
