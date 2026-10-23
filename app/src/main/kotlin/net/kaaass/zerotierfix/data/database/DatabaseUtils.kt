package net.kaaass.zerotierfix.data.database

import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 数据库访问工具类。
 */
object DatabaseUtils {

    /** 数据库读写锁。 */
    @JvmField
    val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

    /** 数据库写锁。 */
    @JvmField
    val writeLock: Lock = readWriteLock.writeLock()

    /** 数据库读锁。 */
    @JvmField
    val readLock: Lock = readWriteLock.readLock()
}
