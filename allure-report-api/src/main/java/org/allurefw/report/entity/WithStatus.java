package org.allurefw.report.entity;

import org.allurefw.Status;

/**
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 31.01.16
 */
public interface WithStatus {

    Status getStatus();

    void setStatus(Status status);

}
