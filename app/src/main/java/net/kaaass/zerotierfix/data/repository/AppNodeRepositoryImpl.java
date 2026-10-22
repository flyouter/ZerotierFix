package net.kaaass.zerotierfix.data.repository;

import net.kaaass.zerotierfix.data.database.AppDatabase;
import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.data.database.entity.AppNode;

import java.util.List;

/**
 * {@link AppNodeRepository} 的 Room 实现。
 */
public final class AppNodeRepositoryImpl implements AppNodeRepository {

    private final AppDatabase database;

    /**
     * 创建本机节点仓库。
     *
     * @param database 应用级 Room 数据库，不能为空
     */
    public AppNodeRepositoryImpl(AppDatabase database) {
        this.database = database;
    }

    @Override
    public String getNodeIdStr() {
        DatabaseUtils.readLock.lock();
        try {
            List<AppNode> nodes = this.database.appNodeDao().loadAll();
            return nodes.isEmpty() ? null : nodes.get(0).getNodeIdStr();
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public void saveNodeId(long nodeId) {
        DatabaseUtils.writeLock.lock();
        try {
            this.database.runInTransaction(() -> {
                List<AppNode> nodes = this.database.appNodeDao().loadAll();
                if (nodes.isEmpty()) {
                    this.database.appNodeDao().insert(
                            new AppNode(nodeId, String.format("%10x", nodeId))
                    );
                } else {
                    AppNode node = nodes.get(0);
                    node.setNodeId(nodeId);
                    node.setNodeIdStr(String.format("%10x", nodeId));
                    this.database.appNodeDao().update(node);
                }
            });
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }
}
