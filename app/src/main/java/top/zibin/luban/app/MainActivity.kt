package top.zibin.luban.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initStorageDirs(context)
    }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                viewModel.onImageSelected(context, uri)
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("图片压缩示例") },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.appStorageDir != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "批量压缩测试",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "输入目录: ${uiState.appStorageDir}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Text(
                            text = "输出目录: ${uiState.outputStorageDir}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        if (uiState.isBatchCompressing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "压缩中: ${uiState.batchProgress}/${uiState.batchTotal}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { 
                                    if (uiState.batchTotal > 0) {
                                        uiState.batchProgress.toFloat() / uiState.batchTotal.toFloat()
                                    } else {
                                        0f
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Button(
                                onClick = { viewModel.batchCompressImages(context) },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始批量压缩")
                            }
                        }
                        
                        if (uiState.batchResult != null) {
                            val result = uiState.batchResult!!
                            Text(
                                text = "压缩完成: 总计 ${result.total}, 成功 ${result.success}, 失败 ${result.failed}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (result.failed == 0) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            if (result.outputDir != null) {
                                Text(
                                    text = "输出目录: ${result.outputDir.absolutePath}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
            
            Button(
                onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("从相册选择图片")
            }

            Button(
                onClick = { viewModel.compressImage(context) },
                enabled = uiState.originalImage != null && !uiState.isCompressing,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (uiState.isCompressing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("压缩中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Compress,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("使用Luban算法压缩")
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.compressionTarget != null && uiState.originalImage != null) {
                CompressionTargetCard(
                    originalWidth = uiState.originalImage!!.width,
                    originalHeight = uiState.originalImage!!.height,
                    target = uiState.compressionTarget!!,
                    formatFileSize = viewModel::formatFileSize
                )
            }

            if (uiState.originalImage != null) {
                ImagePreviewCard(
                    title = "原图",
                    imageInfo = uiState.originalImage!!,
                    formatFileSize = viewModel::formatFileSize,
                    onClick = { viewModel.openImageViewer(uiState.originalImage!!) }
                )
            }

            if (uiState.compressedImage != null) {
                ImagePreviewCard(
                    title = "压缩后",
                    imageInfo = uiState.compressedImage!!,
                    formatFileSize = viewModel::formatFileSize,
                    onClick = { viewModel.openImageViewer(uiState.compressedImage!!) }
                )
            }

            if (uiState.originalImage != null && uiState.compressedImage != null) {
                ComparisonCard(
                    originalSize = uiState.originalImage!!.size,
                    compressedSize = uiState.compressedImage!!.size,
                    formatFileSize = viewModel::formatFileSize
                )
            }
        }
    }

    if (uiState.showImageViewer && uiState.viewingImage != null) {
        FullScreenImageViewer(
            imageInfo = uiState.viewingImage!!,
            onClose = { viewModel.closeImageViewer() }
        )
    }
}

@Composable
fun CompressionTargetCard(
    originalWidth: Int,
    originalHeight: Int,
    target: top.zibin.luban.algorithm.CompressionTarget,
    formatFileSize: (Long) -> String
) {
    val blue50 = Color(0xFFE3F2FD)
    val blue200 = Color(0xFF90CAF9)
    val blue700 = Color(0xFF1976D2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = blue50
        ),
        border = BorderStroke(1.dp, blue200),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = blue700,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Luban压缩参数",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = blue700
                )
            }

            InfoRow(
                label = "原图尺寸",
                value = "$originalWidth × $originalHeight"
            )

            val sizeChanged = target.width != originalWidth || target.height != originalHeight
            InfoRow(
                label = "目标尺寸",
                value = "${target.width} × ${target.height}",
                highlight = sizeChanged
            )

            InfoRow(
                label = "压缩质量",
                value = "60%"
            )

            InfoRow(
                label = "预计大小",
                value = formatFileSize(target.estimatedSizeKb * 1024L)
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) Color(0xFF1976D2) else Color.Black.copy(alpha = 0.87f)
        )
    }
}

@Composable
fun ImagePreviewCard(
    title: String,
    imageInfo: ImageInfo,
    formatFileSize: (Long) -> String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            val model = imageInfo.file ?: imageInfo.uri
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "全屏查看",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "大小: ${formatFileSize(imageInfo.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = "尺寸: ${imageInfo.width} × ${imageInfo.height}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ComparisonCard(
    originalSize: Long,
    compressedSize: Long,
    formatFileSize: (Long) -> String
) {
    val blue50 = Color(0xFFE3F2FD)
    val compressionRatio = compressedSize.toFloat() / originalSize.toFloat()
    val savedSize = originalSize - compressedSize
    val ratioPercent = (1 - compressionRatio) * 100

    val color = when {
        ratioPercent > 50 -> Color(0xFF4CAF50)
        ratioPercent > 20 -> Color(0xFF2196F3)
        else -> Color(0xFFFF9800)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = blue50
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "压缩对比",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ComparisonRow(
                label = "文件大小",
                original = formatFileSize(originalSize),
                compressed = formatFileSize(compressedSize),
                color = color
            )

            ComparisonRow(
                label = "压缩率",
                original = "100%",
                compressed = "${(compressionRatio * 100).toString().take(4)}%",
                color = color
            )

            ComparisonRow(
                label = "节省空间",
                original = "-",
                compressed = formatFileSize(savedSize),
                color = color
            )
        }
    }
}

@Composable
fun ComparisonRow(
    label: String,
    original: String,
    compressed: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = original,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text("→", color = Color.Gray)
            Text(
                text = compressed,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun FullScreenImageViewer(
    imageInfo: ImageInfo,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val model = imageInfo.file ?: imageInfo.uri
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = "全屏图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}