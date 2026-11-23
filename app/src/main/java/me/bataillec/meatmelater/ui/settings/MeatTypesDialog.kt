package me.bataillec.meatmelater.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.bataillec.meatmelater.AppContainer
import me.bataillec.meatmelater.R
import me.bataillec.meatmelater.domain.MeatType

class MeatTypesViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val meatTypes: StateFlow<List<MeatType>> =
        container.meatTypes.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveMeat(
        id: Long,
        name: String,
    ) = viewModelScope.launch { if (name.isNotBlank()) container.meatTypes.save(MeatType(id, name.trim())) }

    /** R1: the meat type is kept (and the refusal reported) while pieces still use it. */
    fun deleteMeat(
        id: Long,
        onRefused: () -> Unit,
    ) = viewModelScope.launch { if (!container.meatTypes.delete(id)) onRefused() }
}

/** R1: meat types are managed from Réglages. */
@Composable
fun MeatTypesDialog(
    meatTypes: List<MeatType>,
    vm: MeatTypesViewModel,
    onClose: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<MeatType?>(null) }
    var editName by remember { mutableStateOf("") }
    var refused by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.meat_types)) },
        text = {
            Column {
                meatTypes.forEach { meat ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        if (editing?.id == meat.id) {
                            OutlinedTextField(
                                editName,
                                { editName = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                vm.saveMeat(meat.id, editName)
                                editing = null
                            }) { Text(stringResource(R.string.save)) }
                        } else {
                            Text(meat.name, Modifier.weight(1f))
                            IconButton(onClick = {
                                editing = meat
                                editName = meat.name
                            }) { Icon(Icons.Filled.Edit, stringResource(R.string.edit)) }
                            IconButton(onClick = { vm.deleteMeat(meat.id) { refused = true } }) {
                                Icon(Icons.Filled.Delete, stringResource(R.string.delete))
                            }
                        }
                    }
                }
                if (refused) {
                    Text(
                        stringResource(R.string.meat_in_use),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        newName,
                        { newName = it },
                        label = { Text(stringResource(R.string.new_meat_type)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        vm.saveMeat(0, newName)
                        newName = ""
                    }) { Text(stringResource(R.string.add)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}
