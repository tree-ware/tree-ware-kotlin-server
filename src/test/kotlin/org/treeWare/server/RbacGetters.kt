package org.treeWare.server

import io.ktor.server.auth.*
import org.treeWare.model.core.*
import org.treeWare.model.operator.rbac.aux.PermissionScope
import org.treeWare.model.operator.rbac.aux.PermissionsAux
import org.treeWare.model.operator.rbac.aux.setPermissionsAux

const val CLARK_KENT_ID = "cc477201-48ec-4367-83a4-7fdbd92f8a6f"
const val LOIS_LANE_ID = "a8aacf55-7810-4b43-afe5-4344f25435fd"

fun addressBookPermitAllRbacGetter(principal: Principal?, mainMeta: MainModel): MainModel {
    val rbac = MutableMainModel(mainMeta)
    rbac.getOrNewRoot()
    setPermissionsAux(rbac, PermissionsAux(all = PermissionScope.SUB_TREE))
    return rbac
}

fun addressBookPermitNoneRbacGetter(principal: Principal?, mainMeta: MainModel): MainModel {
    val rbac = MutableMainModel(mainMeta)
    rbac.getOrNewRoot()
    setPermissionsAux(rbac, PermissionsAux(all = PermissionScope.NONE))
    return rbac
}

fun addressBookPermitClarkKentRbacGetter(principal: Principal?, mainMeta: MainModel): MainModel {
    val rbac = MutableMainModel(mainMeta)
    val rbacRoot = rbac.getOrNewRoot()
    val rbacPersons = getOrNewMutableSetField(rbacRoot, "person")
    val rbacPerson = getNewMutableSetEntity(rbacPersons)
    setUuidSingleField(rbacPerson, "id", CLARK_KENT_ID)
    rbacPersons.addValue(rbacPerson)
    setPermissionsAux(rbacPerson, PermissionsAux(all = PermissionScope.SUB_TREE))
    return rbac
}