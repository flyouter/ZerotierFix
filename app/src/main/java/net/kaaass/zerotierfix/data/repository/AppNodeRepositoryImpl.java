package net.kaaass.zerotierfix.data.repository;

import net.kaaass.zerotierfix.data.database.DatabaseUtils;
import net.kaaass.zerotierfix.data.database.entity.AppNode;
import net.kaaass.zerotierfix.data.database.entity.DaoSession;

import java.util.List;

/**
 * {@link AppNodeRepository} 的 GreenDAO 实现。
 */
public final class AppNodeRepositoryImpl implements AppNodeRepository {

    private final DaoSession daoSession;

    /**
     * 创建本机节点仓库。
     *
     * @param daoSession 应用级数据库会话，不能为空
     */
    public AppNodeRepositoryImpl(DaoSession daoSession) {
        this.daoSession = daoSession;
    }

    @Override
    public String getNodeIdStr() {
        DatabaseUtils.readLock.lock();
        try {
            List<AppNode> nodes = this.daoSession.getAppNodeDao()
                    .queryBuilder()
                    .list();
            return nodes.isEmpty() ? null : nodes.get(0).getNodeIdStr();
        } finally {
            DatabaseUtils.readLock.unlock();
        }
    }

    @Override
    public void saveNodeId(long nodeId) {
        DatabaseUtils.writeLock.lock();
        try {
            this.daoSession.runInTx(() -> {
                List<AppNode> nodes = this.daoSession.getAppNodeDao()
                        .queryBuilder()
                        .list();
                if (nodes.isEmpty()) {
                    this.daoSession.getAppNodeDao().insert(
                            new AppNode(nodeId, String.format("%10x", nodeId))
                    );
                } else {
                    AppNode node = nodes.get(0);
                    node.setNodeId(nodeId);
                    node.setNodeIdStr(String.format("%10x", nodeId));
                    this.daoSession.getAppNodeDao().save(node);
                }
            });
        } finally {
            DatabaseUtils.writeLock.unlock();
        }
    }
}
